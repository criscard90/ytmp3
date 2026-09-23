package com.criscard90.ytmp3.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.criscard90.ytmp3.ui.theme.YtBackground
import com.criscard90.ytmp3.ui.theme.YtRed
import com.criscard90.ytmp3.ui.theme.YtSurfaceVariant
import com.criscard90.ytmp3.ui.theme.YtTextSecondary

/**
 * Barra superiore stile YouTube: logo + wordmark "ytmp3" e campo di ricerca a pillola.
 */
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(YtBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Logo: play bianco su riquadro rosso
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(30.dp)
                    .background(YtRed)
                    .padding(4.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "ytmp3",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))

        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Cerca", color = YtTextSecondary) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = YtTextSecondary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancella", tint = YtTextSecondary)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = YtSurfaceVariant,
                unfocusedContainerColor = YtSurfaceVariant,
                disabledContainerColor = YtSurfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = YtRed,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.White,
                focusedPlaceholderColor = YtTextSecondary,
                unfocusedPlaceholderColor = YtTextSecondary,
                focusedLeadingIconColor = YtTextSecondary,
                unfocusedLeadingIconColor = YtTextSecondary,
                focusedTrailingIconColor = YtTextSecondary,
                unfocusedTrailingIconColor = YtTextSecondary,
            ),
        )
    }
}
