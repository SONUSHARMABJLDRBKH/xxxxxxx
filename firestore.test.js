const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_UID = "admin_uid_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("wallets").doc(ALICE_UID).get());
  await assertFails(unauth.collection("quiz_questions").get());
  await assertFails(unauth.collection("rewards").get());
  await assertFails(
    unauth.collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      txType: "WELCOME_BONUS",
      amount: 10,
      status: "COMPLETED",
      description: "Test",
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile isolation and anti-privilege-escalation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Kumar",
      email: "alice@example.com",
      language: "en",
      darkMode: true,
      notificationsEnabled: true,
      role: "user",
      blocked: false,
      welcomeBonusClaimed: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cannot create profile with role: admin
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      language: "en",
      role: "admin",
      blocked: false,
      welcomeBonusClaimed: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user read & update denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked" }));

  // Owner cannot escalate own role or change createdAt
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ role: "admin", updatedAt: new Date(Date.now() - 500) }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ createdAt: new Date(Date.now() - 500), updatedAt: new Date(Date.now() - 500) }));

  // Owner can update allowed preference fields
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      language: "hi",
      darkMode: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Wallet creation cap, non-negative balance invariant, and query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Cannot create wallet starting with 99999 coins
  await assertFails(
    aliceDb.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      availableCoins: 99999,
      totalEarnedCoins: 99999,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid initial wallet with 10 welcome coins
  await assertSucceeds(
    aliceDb.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      availableCoins: 10,
      totalEarnedCoins: 10,
      welcomeBonusCoins: 10,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cannot set negative balance
  await assertFails(
    aliceDb.collection("wallets").doc(ALICE_UID).update({
      availableCoins: -5,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot read Alice's wallet
  await assertFails(bobDb.collection("wallets").doc(ALICE_UID).get());

  // Transactions: filtered query succeeds, unfiltered fails
  await assertSucceeds(
    aliceDb.collection("transactions").doc("tx_welcome").set({
      userId: ALICE_UID,
      txType: "WELCOME_BONUS",
      amount: 10,
      status: "COMPLETED",
      description: "Welcome bonus",
      createdAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("transactions").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("transactions").get());
});

test("Quiz questions, rewards catalog, and admin role enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "farginagabhai@gmail.com", email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof", { email: "farginagabhai@gmail.com", email_verified: false }).firestore();
  const now = new Date(Date.now() - 3000);

  const qPayload = {
    questionClass: 10,
    subject: "Mathematics",
    chapter: "Trigonometry",
    questionText: "sin^2(theta) + cos^2(theta) = ?",
    optionA: "0",
    optionB: "1",
    optionC: "2",
    optionD: "10",
    correctOption: "B",
    explanation: "Basic trigonometric identity.",
    source: "Bihar Board Class 10 Textbook",
    examYear: "2023",
    createdBy: ADMIN_UID,
    createdAt: now,
    updatedAt: now,
  };

  // Normal user and unverified email spoof cannot create quiz questions
  await assertFails(aliceDb.collection("quiz_questions").doc("q_1").set(qPayload));
  await assertFails(spoofedAdminDb.collection("quiz_questions").doc("q_1").set(qPayload));

  // Verified bootstrapped admin can create and update
  await assertSucceeds(adminDb.collection("quiz_questions").doc("q_1").set(qPayload));
  await assertSucceeds(aliceDb.collection("quiz_questions").get());

  // Reward submissions: user can create PENDING, cannot self-approve, admin can approve
  const subPayload = {
    userId: ALICE_UID,
    username: "Alice",
    rewardId: "watch_1",
    rewardTitle: "Normal Watch",
    submissionType: "STORE_REDEMPTION",
    coinCost: 30000,
    upiOrAddress: "Patna, Bihar",
    status: "PENDING",
    createdAt: now,
    updatedAt: now,
  };
  await assertSucceeds(aliceDb.collection("reward_submissions").doc("sub_1").set(subPayload));
  await assertFails(aliceDb.collection("reward_submissions").doc("sub_1").update({ status: "APPROVED", updatedAt: new Date(Date.now() - 500) }));
  await assertSucceeds(adminDb.collection("reward_submissions").doc("sub_1").update({ status: "APPROVED", adminNote: "Verified", updatedAt: new Date(Date.now() - 500) }));
});

test("Reject future timestamps and oversized document IDs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const futureDate = new Date(Date.now() + 120000);
  await assertFails(
    aliceDb.collection("daily_bonuses").doc("b_1").set({
      userId: ALICE_UID,
      claimDate: "2026-11-20",
      coins: 25,
      createdAt: futureDate,
    })
  );
  await assertFails(
    aliceDb.collection("daily_bonuses").doc("bad id with spaces!").set({
      userId: ALICE_UID,
      claimDate: "2026-11-20",
      coins: 25,
      createdAt: new Date(Date.now() - 1000),
    })
  );
});
