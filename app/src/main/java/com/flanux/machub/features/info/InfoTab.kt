package com.flanux.machub.features.info

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoTab() {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Information") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // About MAC
            InfoSection(
                title = "About MAC Pokhara",
                icon = Icons.Default.Info
            ) {
                Text(
                    "Mount Annapurna Campus (MAC) is a premier educational institution " +
                    "in Pokhara, Nepal, offering B.Sc. CSIT and other programs affiliated " +
                    "with Tribhuvan University.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Contact Information
            InfoSection(
                title = "Contact Information",
                icon = Icons.Default.Phone
            ) {
                ContactItem(
                    icon = Icons.Default.LocationOn,
                    label = "Address",
                    value = "Prithvi Chowk, Pokhara, Nepal"
                )
                ContactItem(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = "+977-61-532211"
                )
                ContactItem(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = "info@macpokhara.edu.np"
                )
            }
            
            // Quick Links
            InfoSection(
                title = "Quick Links",
                icon = Icons.Default.Star
            ) {
                LinkButton(
                    label = "Official Website",
                    url = "https://www.macpokhara.edu.np",
                    icon = Icons.Default.Face,
                    onClick = { openUrl(context, it) }
                )
                LinkButton(
                    label = "Facebook Page",
                    url = "https://www.facebook.com/mountannapurnacampus",
                    icon = Icons.Default.Share,
                    onClick = { openUrl(context, it) }
                )
                LinkButton(
                    label = "Tribhuvan University",
                    url = "https://tuexam.edu.np",
                    icon = Icons.Default.AccountBox,
                    onClick = { openUrl(context, it) }
                )
            }
            
            // App Information
            InfoSection(
                title = "App Information",
                icon = Icons.Default.Settings
            ) {
                Text(
                    "macHub - Unofficial student companion app for MAC Pokhara",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Version: 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Developed by students, for students. All data is scraped from " +
                    "the official MAC website and is updated regularly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // GitHub Link
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Open Source",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "View source code on GitHub",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(
                        onClick = { openUrl(context, "https://github.com/flanux/macHub") }
                    ) {
                        Icon(Icons.Default.Face, "GitHub")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun ContactItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LinkButton(
    label: String,
    url: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (String) -> Unit
) {
    OutlinedButton(
        onClick = { onClick(url) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(label)
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
