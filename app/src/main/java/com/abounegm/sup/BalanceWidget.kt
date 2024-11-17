package com.abounegm.sup

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.Date

class BalanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        // In this method, load data needed to render the AppWidget.
        // Use `withContext` to switch to another thread for long running
        // operations.

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(8.dp)
                    .background(Color.White)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                WidgetBody(context)
            }
        }
    }

    @Composable
    private fun WidgetBody(context: Context) {
        val store = BalanceStore(context)
        val remainingAmount = store.getLimit.map { it?.remainingLimit }.collectAsState(initial = 0f)
        val balance = store.getBalance.collectAsState(initial = 0f)
        val lastUpdated = store.getLastUpdated.collectAsState(initial = Date.from(Instant.EPOCH))
        val cardInfo = store.getCardInfo.collectAsState(initial = CardData.None)
        val timeFormatter = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)
        val isOld = Duration.between(lastUpdated.value.toInstant(), Instant.now()).toHours() > 1
        var updating by remember { mutableStateOf(false) }

        Column(modifier = GlanceModifier.fillMaxHeight()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = timeFormatter.format(lastUpdated.value),
                    style = TextStyle(
                        fontSize = 10.sp,
                        color =
                        if (isOld) ColorProvider(MaterialTheme.colorScheme.error)
                        else TextDefaults.defaultTextColor,
                    ),
                    modifier = GlanceModifier.padding(5.dp, 0.dp)
                )
                if (updating) {
                    CircularProgressIndicator(
                        GlanceModifier.size(16.dp),
                        color = ColorProvider(Color.Blue),
                    )
                } else {
                    CircleIconButton(
                        imageProvider = ImageProvider(R.drawable.baseline_refresh_24),
                        contentDescription = "refresh",
                        onClick = {
                            updating = true
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    store.updateValues()
                                } catch (e: Exception) {
                                    println(e)
                                } finally {
                                    updating = false
                                }
                            }
                        },
                        contentColor = ColorProvider(Color.Black),
                        backgroundColor = ColorProvider(Color.White),
                        modifier = GlanceModifier
                            .width(20.dp)
                            .height(16.dp)
                            .padding(0.dp, 0.dp, 4.dp, 0.dp)
                    )
                }
            }
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${(remainingAmount.value ?: balance.value).toInt()} ₽",
                    style = TextStyle(fontSize = 26.sp)
                )
            }

            Row {
                if (remainingAmount.value != null) {
                    Text(
                        text = context.getString(
                            R.string.balance,
                            balance.value.toInt().toString()
                        ),
                        style = TextStyle(fontSize = 12.sp),
                    )
                } else if (cardInfo.value is CardData.Virtual) {
                    Text(
                        text = context.getString(
                            R.string.expires,
                            (cardInfo.value as CardData.Virtual).card.expiryDate
                        ),
                        style = TextStyle(fontSize = 10.sp),
                    )
                }
            }
        }
    }
}