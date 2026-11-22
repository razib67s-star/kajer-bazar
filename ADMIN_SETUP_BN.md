# কাজের বাজার — প্রথম Admin অ্যাকাউন্ট চালু

Admin dashboard শুধু `profiles.role = 'admin'` অ্যাকাউন্টে দেখা যাবে। সাধারণ OTP signup দিয়ে কেউ নিজেকে Admin করতে পারবে না।

## ধাপ ১: প্রথমে নিজের ফোনে OTP signup করুন
1. Supabase Phone Auth এবং SMS provider configure করুন (BACKEND_SETUP_BN.md দেখুন)।
2. অ্যাপ থেকে নিজের নিয়ন্ত্রিত মোবাইল নম্বর দিয়ে OTP যাচাই করে account তৈরি করুন।
3. OTP login সফল হয়েছে নিশ্চিত করুন।

## ধাপ ২: Supabase-এ নিজের account ID খুঁজুন
Supabase Dashboard → SQL Editor-এ এই query চালান। নিজের নম্বরটি international format-এ বসান, যেমন `+8801XXXXXXXXX`:

```sql
select id, phone, created_at
from auth.users
where phone = '+8801XXXXXXXXX';
```

ফলাফলে নিজের account এবং নম্বর ঠিক আছে কি না যাচাই করুন। অন্য কারও account নির্বাচন করবেন না।

## ধাপ ৩: প্রথম Admin role দিন
শুধু নিজের account নিশ্চিত হওয়ার পর নিচের query-তে একই নম্বর বসিয়ে চালান:

```sql
update public.profiles
set role = 'admin'
where id = (
  select id from auth.users
  where phone = '+8801XXXXXXXXX'
);
```

তারপর verify করুন:

```sql
select p.id, u.phone, p.role
from public.profiles p
join auth.users u on u.id = p.id
where u.phone = '+8801XXXXXXXXX';
```

ফলাফলে role = admin দেখালে অ্যাপ থেকে logout করে আবার OTP login করুন। Admin dashboard-এ pending employer requests এবং pending jobs review করা যাবে।

## সতর্কতা
- এই কাজ শুধু Supabase project owner করবেন।
- অন্য কাউকে Admin করবেন না; Admin সব ব্যবহারকারী, কাজ ও আবেদন moderation করতে পারেন।
- SQL Editor-এ service-role key বা password লিখবেন না।
- এই প্রথম Admin account setup না হওয়া পর্যন্ত employer request/job moderation আটকে থাকবে।
