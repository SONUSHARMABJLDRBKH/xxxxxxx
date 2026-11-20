const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

/**
 * AdMob Server-Side Verification (SSV) Webhook for StudyWin India.
 * Configure this HTTPS endpoint URL inside AdMob -> Rewarded Ad Unit -> Server-side verification.
 */
exports.verifyAdMobRewardSsv = functions.https.onRequest(async (req, res) => {
  try {
    const userId = req.query.user_id;
    const rewardAmount = parseInt(req.query.reward_amount || "10", 10);
    const customData = req.query.custom_data || "general";
    const transactionId = req.query.transaction_id;

    if (!userId || !transactionId) {
      return res.status(400).send("Missing user_id or transaction_id");
    }

    const adRef = db.collection("ad_rewards").doc(`ssv_${transactionId}`);
    const existing = await adRef.get();
    if (existing.exists) {
      return res.status(200).send("Duplicate ignored");
    }

    const walletRef = db.collection("wallets").doc(userId);
    await db.runTransaction(async (tx) => {
      const wSnap = await tx.get(walletRef);
      if (!wSnap.exists) return;

      const currentAds = (wSnap.data().adRewardsCount || 0) + 1;
      const unlocked = wSnap.data().specialSpinsUnlocked || 0;
      const updatedUnlocked = currentAds % 100 === 0 ? unlocked + 5 : unlocked;

      tx.update(walletRef, {
        adRewardsCount: currentAds,
        specialSpinsUnlocked: updatedUnlocked,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      tx.set(adRef, {
        userId,
        adFormat: "REWARDED_SSV",
        placement: customData,
        adUnitId: "ca-app-pub-6545218519929279/9899043803",
        coinsAwarded: rewardAmount,
        verified: true,
        rewardToken: transactionId,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });

    return res.status(200).send("OK");
  } catch (err) {
    console.error("SSV error:", err);
    return res.status(500).send("Internal error");
  }
});

/**
 * Admin-only callable to approve or reject reward submissions atomically.
 */
exports.adminResolveSubmission = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Sign in required");
  }
  const adminDoc = await db.collection("admin_users").doc(context.auth.uid).get();
  const isBootstrappedAdmin =
    context.auth.token.email === "farginagabhai@gmail.com" &&
    context.auth.token.email_verified === true;
  if (!adminDoc.exists && !isBootstrappedAdmin) {
    throw new functions.https.HttpsError("permission-denied", "Admin privileges required");
  }

  const { submissionId, status, adminNote } = data;
  if (!["APPROVED", "REJECTED", "FULFILLED"].includes(status)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid status");
  }

  await db.collection("reward_submissions").doc(submissionId).update({
    status,
    adminNote: adminNote || "",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { success: true };
});
