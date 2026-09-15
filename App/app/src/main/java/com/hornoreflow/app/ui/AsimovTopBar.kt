package com.hornoreflow.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hornoreflow.app.R

@Composable
fun AsimovTopBar(
    title: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_asimov_logo),
                contentDescription = null,
                modifier = Modifier.width(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "ASIMOV",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(title, style = MaterialTheme.typography.headlineSmall)
            }
        }
        trailing?.invoke()
    }
}
