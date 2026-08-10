package com.autka.feature.external

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.autka.R
import com.autka.core.model.SearchFilter

/**
 * "Continue your search on…" row. Shown under thin/empty results (or always as a
 * footer): marketplace chips open their own pre-filled search, while the optional web
 * chip searches the same relevant marketplace domains through Brave Search. Autka does
 * not ingest either path's content.
 */
@Composable
fun MarketplaceLinksRow(
    filter: SearchFilter,
    modifier: Modifier = Modifier,
    affiliateId: String? = null,
) {
    val context = LocalContext.current
    val links = remember(filter, affiliateId) {
        MarketplaceSearchLinks.all(filter, affiliateId)
    }
    val webSearchUrl = remember(filter) { MarketplaceWebSearch.url(filter) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.continue_search_on),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            webSearchUrl?.let { url ->
                item(key = "web-search") {
                    AssistChip(
                        onClick = { openExternalLink(context, url) },
                        label = { Text(stringResource(R.string.search_web)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }
            items(links, key = { it.sourceId }) { link ->
                AssistChip(
                    onClick = { openExternalLink(context, link.url) },
                    label = { Text(link.displayName) },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }
        }
    }
}

private fun openExternalLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_browser, Toast.LENGTH_SHORT).show()
    }
}
