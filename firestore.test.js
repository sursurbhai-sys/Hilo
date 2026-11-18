const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, name = "Alice") {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: name,
    trophies: 120,
    xp: 250,
    coins: 400,
    wins: 3,
    gamesPlayed: 5,
    captures: 9,
    powerUpsUsed: 7,
    sixesRolled: 12,
    tokensHome: 10,
    turboCount: 2,
    shieldCount: 2,
    swapCount: 1,
    luckySixCount: 1,
    freezeCount: 1,
    magnetCount: 1,
    createdAt: now,
    updatedAt: now,
  };
}

function validMissionPayload(date = "2026-11-17") {
  const now = new Date(Date.now() - 2000);
  return {
    missionType: "CAPTURE_TOKENS",
    title: "Hunter Strike",
    description: "Capture 3 rival tokens in matches",
    missionDate: date,
    progress: 1,
    target: 3,
    rewardXp: 200,
    rewardCoins: 120,
    claimed: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validLeaderboardPayload(uid, name = "Alice", trophies = 120) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: name,
    trophies,
    wins: 3,
    gamesPlayed: 5,
    captures: 9,
    tier: "Silver",
    createdAt: now,
    updatedAt: now,
  };
}

function validRoomPayload(hostUid, roomCode = "LUDO7X") {
  const now = new Date(Date.now() - 2000);
  return {
    hostId: hostUid,
    roomCode,
    status: "WAITING",
    playerUids: [hostUid],
    playerNames: ["Alice"],
    playerColors: ["RED"],
    currentTurnIndex: 0,
    lastDiceValue: 0,
    turnPhase: "ROLL",
    winnerUid: "",
    tokenPositions: Array(16).fill(-1),
    shieldTurns: [0, 0, 0, 0],
    frozenTurns: [0, 0, 0, 0],
    orbTiles: [5, 18, 31, 44],
    orbTypes: ["TURBO_BOOST", "SHIELD", "SWAP", "MAGNET"],
    lastEventText: "Room created. Waiting for players...",
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across users, missions, leaderboard, and rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertFails(unauthDb.collection("leaderboard").where("trophies", ">=", 0).get());
  await assertFails(unauthDb.collection("rooms").where("status", "==", "WAITING").get());
});

test("User profile: owner create, partial update, cross-user denial, and immutable checks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));

  // Reject ghost field / @DocumentId id injection
  await assertFails(
    aliceDb.collection("users").doc("alice_other").set({
      ...validProfilePayload(ALICE_UID),
      id: "alice_other",
    })
  );

  // Valid partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      trophies: 150,
      coins: 520,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt tamper
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ coins: 9999, updatedAt: new Date(Date.now() - 500) }));
});

test("Daily missions: subcollection list without existing() filter, progress update, claim lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Empty collection list succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("missions").get());

  const missionRef = aliceDb.collection("users").doc(ALICE_UID).collection("missions").doc("2026-11-17_CAPTURE_TOKENS");
  await assertSucceeds(missionRef.set(validMissionPayload("2026-11-17")));

  // Cannot claim before target reached
  await assertFails(
    missionRef.update({
      claimed: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Update progress to target
  await assertSucceeds(
    missionRef.update({
      progress: 3,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Claim now succeeds
  await assertSucceeds(
    missionRef.update({
      claimed: true,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Terminal state lock: cannot update after claimed
  await assertFails(
    missionRef.update({
      progress: 4,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Cross-user access denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("missions").get());
});

test("Leaderboard: owner writes own entry; signed-in query aligned with rules succeeds; unfiltered fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardPayload(ALICE_UID, "Alice", 220))
  );
  await assertSucceeds(
    bobDb.collection("leaderboard").doc(BOB_UID).set(validLeaderboardPayload(BOB_UID, "Bob", 310))
  );

  // Cannot overwrite another player's leaderboard document
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({
      trophies: 0,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Matches Kotlin query: whereGreaterThanOrEqualTo("trophies", 0).orderBy("trophies", DESCENDING).limit(50)
  await assertSucceeds(
    aliceDb.collection("leaderboard").where("trophies", ">=", 0).orderBy("trophies", "desc").limit(50).get()
  );

  // Unfiltered list query denied
  await assertFails(aliceDb.collection("leaderboard").get());
});

test("Multiplayer rooms: create, join, start, turn update, finish lock, and non-participant isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const roomRefAlice = aliceDb.collection("rooms").doc("LUDO7X");
  await assertSucceeds(roomRefAlice.set(validRoomPayload(ALICE_UID, "LUDO7X")));

  // Open waiting room list query matches rule
  await assertSucceeds(bobDb.collection("rooms").where("status", "==", "WAITING").limit(20).get());

  // Unfiltered rooms query fails
  await assertFails(bobDb.collection("rooms").get());

  // Bob joins waiting room
  await assertSucceeds(
    bobDb.collection("rooms").doc("LUDO7X").update({
      playerUids: [ALICE_UID, BOB_UID],
      playerNames: ["Alice", "Bob"],
      playerColors: ["RED", "YELLOW"],
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Non-host Bob cannot start match
  await assertFails(
    bobDb.collection("rooms").doc("LUDO7X").update({
      status: "ACTIVE",
      currentTurnIndex: 0,
      turnPhase: "ROLL",
      orbTiles: [5, 18, 31, 44],
      orbTypes: ["TURBO_BOOST", "SHIELD", "SWAP", "MAGNET"],
      lastEventText: "Started",
      updatedAt: new Date(Date.now() - 350),
    })
  );

  // Host Alice starts match
  await assertSucceeds(
    roomRefAlice.update({
      status: "ACTIVE",
      currentTurnIndex: 0,
      turnPhase: "ROLL",
      orbTiles: [5, 18, 31, 44],
      orbTypes: ["TURBO_BOOST", "SHIELD", "SWAP", "MAGNET"],
      lastEventText: "Match started!",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Non-participant Charlie cannot read or update active room
  await assertFails(charlieDb.collection("rooms").doc("LUDO7X").get());
  await assertFails(
    charlieDb.collection("rooms").doc("LUDO7X").update({
      lastDiceValue: 6,
      updatedAt: new Date(Date.now() - 250),
    })
  );

  // Participant Bob submits move and finishes match
  const nextPositions = Array(16).fill(-1);
  nextPositions[4] = 56;
  await assertSucceeds(
    bobDb.collection("rooms").doc("LUDO7X").update({
      status: "FINISHED",
      currentTurnIndex: 1,
      lastDiceValue: 6,
      turnPhase: "FINISHED",
      winnerUid: BOB_UID,
      tokenPositions: nextPositions,
      shieldTurns: [0, 0, 0, 0],
      frozenTurns: [0, 0, 0, 0],
      orbTiles: [18, 31],
      orbTypes: ["SHIELD", "SWAP"],
      lastEventText: "Bob won the match!",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Terminal state lock: no updates after FINISHED
  await assertFails(
    roomRefAlice.update({
      status: "ACTIVE",
      lastDiceValue: 1,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});
