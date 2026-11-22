# Kajer Bazar পেমেন্ট প্রস্তুতি

## বর্তমান অবস্থা
- এই পর্যায়ে কোনো লাইভ পেমেন্ট নেওয়া হয় না।
- payment_orders টেবিলে প্রতিটি পেমেন্ট অর্ডার রাখা হয়।
- payment_ledger টেবিল ভবিষ্যতের চার্জ, ১০% প্ল্যাটফর্ম কমিশন, কর্মীর পাওনা এবং রিফান্ডের হিসাব রাখার জন্য।
- সব অর্থ BDT পয়সায় (paisa) সংরক্ষিত হয়।
- পেমেন্ট রেকর্ডের লেখা/পরিবর্তন শুধু সার্ভার-সাইড থেকে হবে; নিয়মিত ব্যবহারকারী শুধু নিজের সংশ্লিষ্ট অর্ডার ও লেজার দেখতে পারবেন।
- create-payment-order Edge Function লগইন করা employer-এর মালিকানাধীন কাজ এবং নির্বাচিত worker যাচাই করে শুধু pending sandbox অর্ডার তৈরি করে। এটি টাকা কাটে না এবং অর্ডারকে paid করে না।

## অর্ডার তৈরির API
POST /functions/v1/create-payment-order

Header:
- Authorization: Bearer <user access token>
- Content-Type: application/json

Body:
```json
{
  "job_id": "<job UUID>",
  "application_id": "<selected application UUID>",
  "idempotency_key": "<unique random key>"
}
```

একই idempotency key পুনরায় পাঠালে একই অর্ডার ফেরত দেওয়া হবে (একই employer ও job হলে)।

## কমিশনের হিসাব
ডিফল্ট কমিশন ১০% (1000 basis points)। উদাহরণ: কাজের মজুরি ১,০০০ টাকা হলে কমিশন ১০০ টাকা এবং worker net ৯০০ টাকা। এটি কেবল হিসাবের প্রস্তুতি; এখনো কোনো টাকা সংগ্রহ বা বিতরণ হচ্ছে না। কমিশন কে বহন করবে এবং টাকা কীভাবে কর্মীকে দেওয়া হবে—লাইভ চালুর আগে তা চূড়ান্ত করতে হবে।

## লাইভ চালুর আগে যা বাকি
1. SSLCOMMERZ বা অনুমোদিত পেমেন্ট গেটওয়ের merchant onboarding সম্পন্ন করা।
2. Sandbox Store ID/Password পাওয়া এবং Supabase Edge Function Secrets-এ নিরাপদে রাখা; কখনো Android অ্যাপ বা GitHub-এ নয়।
3. Gateway session initiation, success/fail/cancel/IPN এবং server-side transaction validation যুক্ত করা।
4. একই transaction যেন একাধিকবার settle না হয়—idempotency, amount/currency matching এবং refund/reconciliation পরীক্ষা করা।
5. Gateway-এর অনুমোদিত settlement/payout model এবং marketplace কমিশন গ্রহণের শর্ত নিশ্চিত করা।

**গুরুত্বপূর্ণ:** browser redirect বা app response দেখে কখনো payment সফল ধরা যাবে না। Gateway-এর server-to-server validation সফল হলেই live payment paid করা যাবে।
