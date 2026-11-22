# কাজের বাজার — Backend সংযোগ নির্দেশিকা

## Supabase ভিত্তি
প্রকল্পটি Supabase PostgreSQL, Auth এবং Row Level Security (RLS) ব্যবহার করে। Android-এ শুধু publishable key ব্যবহৃত হয়; service-role key কখনো অ্যাপে রাখা যাবে না।

## ইতিমধ্যে Supabase-এ প্রয়োগ করা হয়েছে
- profiles, jobs, applications, completion_requests এবং payment tables।
- RLS নীতি এবং worker default role; ক্লায়েন্ট নিজে employer/admin হতে পারে না।
- Phone Auth OTP-এর পর স্বয়ংক্রিয় profile তৈরি; ফোন নম্বর profile-এ সংরক্ষণের trigger।
- Employer verification request; admin approval ছাড়া employer role দেওয়া হয় না।
- Job lifecycle RPC: কাজ review-তে পাঠানো, admin moderation, worker application, application withdraw, worker selection, completion request, employer confirmation/dispute এবং overdue completion Admin Review-তে পাঠানো।
- Payment order server-side function; বর্তমানে sandbox/pending only এবং টাকা সংগ্রহ করে না।
- ১০% commission employer-এর কাছ থেকে agreed worker wage-এর অতিরিক্ত; worker পুরো agreed wage পান।

## Android অ্যাপে যুক্ত করা হয়েছে
- বাংলাদেশি মোবাইল নম্বরের OTP request/verification flow।
- OTP session থেকে authenticated profile ও published jobs পড়া।
- Worker job application RPC ব্যবহার করে আবেদন করতে পারে।
- Worker employer হওয়ার আবেদন পাঠাতে পারে।
- Admin-approved employer draft job তৈরি করে moderation-এ পাঠাতে পারে।

## OTP চালু করতে Supabase Dashboard-এ প্রয়োজনীয় কাজ
১. Authentication → Sign In / Providers-এ Phone provider enable করুন।
২. Authentication → SMS Provider-এ অনুমোদিত SMS provider নির্বাচন করুন।
৩. provider account-এর SMS credentials Supabase Dashboard-এর secret/configuration অংশে দিন; GitHub বা Android app-এ নয়।
৪. OTP expiry, resend interval এবং rate limit নিরাপদ মানে রাখুন।
৫. আপনার নিজের নম্বর দিয়ে test করুন; SMS খরচ/দেশভিত্তিক delivery provider অনুযায়ী হবে।

এই configuration ChatGPT থেকে করা হয়নি; Supabase Dashboard-এ project owner-কে provider account ও secret দিতে হবে। SMS provider configure না থাকলে Android OTP screen compile হলেও আসল SMS যাবে না।

## Admin প্রস্তুতি
- প্রথম admin account নিরাপদে Supabase SQL Editor থেকে manually নির্ধারণ করতে হবে। কাউকে admin করার জন্য public signup endpoint নেই।
- Admin account দিয়ে employer request approve/reject এবং job moderation করার secure database RPC প্রস্তুত।
- Admin web dashboard এখনো তৈরি হয়নি; review functions database-এ আছে, UI নেই।
- ৩ দিনের overdue completion-কে Admin Review-তে পাঠানোর function আছে, কিন্তু scheduled invocation/cron এখনো configure হয়নি।

## এখনো বাকি / production-এর আগে
- Android-এর encrypted session persistence, refresh-token lifecycle ও logout invalidation।
- Profile edit, ID verification/document upload এবং admin dashboard UI।
- Admin notification/FCM, report/block, ratings, support and audit UI।
- Overdue completion scheduler, disputes evidence and admin resolution.
- OTP provider config and real SMS end-to-end test.
- Payment gateway merchant onboarding, gateway callbacks/IPN and settlement.
- Security review, privacy policy, terms, refund/cancellation policy, release signing and device tests.

## নিরাপত্তা
- Play Protect বা Android Auto Blocker বন্ধ করবেন না।
- service-role key, database password, SMS secret বা payment secret কখনো APK/GitHub/chat-এ দেবেন না।
- নতুন account worker হিসেবে শুরু হয়; employer role admin approval ছাড়া দেওয়া হয় না।
