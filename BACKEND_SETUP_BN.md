# কাজের বাজার — Backend সংযোগ নির্দেশিকা

## নির্বাচিত ভিত্তি
প্রথম backend হিসেবে Supabase (managed PostgreSQL + Auth + Row Level Security) নির্বাচন করা হয়েছে। এতে Android অ্যাপ থেকে সরাসরি ডেটাবেসে অনিরাপদ প্রবেশের বদলে authenticated access ও database policy ব্যবহার করা যাবে।

## এই commit-এ কী যোগ হয়েছে
- `supabase/migrations/202611220001_initial_schema.sql`: প্রাথমিক profiles, jobs, applications ও completion_requests schema।
- Row Level Security (RLS) চালু এবং প্রাথমিক access policy।
- নতুন অ্যাকাউন্ট ডিফল্টে worker; employer/admin role কেবল trusted admin/server workflow থেকে দিতে হবে।
- completion request-এ ৩ দিনের response deadline রাখা হয়েছে। সময় পার হলে স্বয়ংক্রিয়ভাবে সম্পন্ন/কমিশন আদায় নয়; পরবর্তী backend job-কে Admin Review-তে পাঠাতে হবে।

## এখনো চালু হয়নি
এই SQL ফাইল repository-তে রাখা মানে Supabase project তৈরি বা সংযুক্ত হয়েছে—এমন নয়। এখনো live API, OTP/SMS, notification, admin dashboard বা payment gateway নেই। Completion timeout scheduler ও server-side dispute functions-ও পরের ধাপ।

## Supabase project তৈরি করতে যা লাগবে
১. Supabase-এ নিজের account দিয়ে project তৈরি করুন।
২. Database region হিসেবে Bangladesh-এর কাছাকাছি উপলভ্য region বেছে নিন।
৩. SQL Editor-এ migration ফাইলের সম্পূর্ণ SQL চালান, অথবা Supabase CLI দিয়ে migration apply করুন।
৪. Project URL ও publishable/anon key Android app-এ ব্যবহার করার আগে configuration layer তৈরি ও যাচাই করতে হবে।
৫. **service_role key, database password, SMS secret বা payment secret Android app, GitHub source, screenshot বা ChatGPT-তে দেবেন না।** এগুলো কেবল secure server/secret manager-এ থাকবে।

## গুরুত্বপূর্ণ নিরাপত্তা নোট
- বর্তমান migration একটি প্রাথমিক foundation; production security review-এর বিকল্প নয়।
- employer role এখনো স্বয়ংক্রিয়ভাবে দেওয়া হয় না; trusted admin verification দরকার।
- job publishing/moderation-এর পূর্ণ workflow এখনো তৈরি হয়নি।
- completion confirmation, dispute review, ৩ দিনের reminder/Admin Review এবং ১০% employer-paid commission server-side transaction হিসেবে implement ও test না করা পর্যন্ত payment চালু করা যাবে না।
- Real payment-এর আগে merchant approval, refund/cancellation policy, audit log ও legal review দরকার।
