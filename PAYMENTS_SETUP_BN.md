# Kajer Bazar — পেমেন্ট প্রস্তুতি

## বর্তমান অবস্থা
- কোনো লাইভ পেমেন্ট নেওয়া হয় না।
- payment_orders ও payment_ledger-এ BDT পয়সা হিসেবে হিসাব রাখা হয়।
- create-payment-order Edge Function যাচাইকৃত employer, নিজের job এবং selected worker application যাচাই করে pending sandbox order তৈরি করে।
- sandbox order তৈরি মানে টাকা কাটা বা paid হওয়া নয়।
- payment writes client থেকে বন্ধ; trusted server-side function ব্যবহার করতে হবে।

## অনুমোদিত কমিশন নীতি
কর্মীর agreed wage থেকে কমিশন কাটা হবে না। নিয়োগকর্তা agreed wage-এর অতিরিক্ত ১০% কমিশন দেবেন।

উদাহরণ:
- কর্মীর agreed wage: ৳১,০০০
- নিয়োগকর্তার ১০% কমিশন: ৳১০০
- নিয়োগকর্তার মোট payable: ৳১,১০০
- কর্মীর পাওনা: ৳১,০০০

Database fields:
- amount_paisa = worker agreed wage
- commission_paisa = employer-paid platform commission
- employer_total_paisa = wage + commission
- worker_net_paisa = full agreed wage

## API
POST /functions/v1/create-payment-order

Headers:
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

## লাইভ চালুর আগে
1. SSLCOMMERZ বা অন্য অনুমোদিত provider-এর marketplace/collection model লিখিতভাবে অনুমোদন নিতে হবে।
2. Merchant onboarding ও sandbox credentials সংগ্রহ করতে হবে।
3. Gateway checkout, success/fail/cancel, IPN/webhook এবং server-side transaction validation তৈরি করতে হবে।
4. Amount, currency, transaction ID, signature, duplicate callbacks, refund/reconciliation পরীক্ষা করতে হবে।
5. Worker payout/disbursement এবং escrow/holding আইনগত ও provider policy অনুযায়ী অনুমোদিত কি না যাচাই করতে হবে।
6. Secret শুধু Supabase Edge Function Secrets-এ থাকবে; Android/GitHub-এ নয়।

**Redirect বা app-এর response দেখে payment success ধরা যাবে না।** Gateway server-to-server validation ছাড়া কোনো order paid করা যাবে না।
