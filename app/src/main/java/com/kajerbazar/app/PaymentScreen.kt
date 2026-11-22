package com.kajerbazar.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Payment readiness panel. Live checkout is intentionally unavailable until
 * merchant approval, authenticated job/application flows, and server verification exist.
 */
@Composable
fun PaymentScreen(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FB))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("পেমেন্ট ব্যবস্থা", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("পরীক্ষামূলক অর্ডার তৈরির সার্ভার প্রস্তুত আছে।")
            Text("বর্তমান অবস্থা: শুধু Sandbox / Pending")
            Text("• এখন কোনো টাকা কাটা বা গ্রহণ করা হয় না।")
            Text("• অর্ডার তৈরির জন্য যাচাইকৃত নিয়োগদাতা লগইন, চলমান কাজ এবং নির্বাচিত কর্মীর আবেদন লাগবে।")
            Text("• বাস্তব পেমেন্ট চালুর আগে অনুমোদিত মার্চেন্ট অ্যাকাউন্ট, সার্ভার-সাইড লেনদেন যাচাই এবং IPN পরীক্ষা প্রয়োজন।")
            Spacer(Modifier.height(2.dp))
            Text(
                "নিরাপত্তা: অ্যাপে বা GitHub-এ কোনো পেমেন্ট গেটওয়ের গোপন চাবি রাখা হবে না।",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF285078)
            )
        }
    }
}
