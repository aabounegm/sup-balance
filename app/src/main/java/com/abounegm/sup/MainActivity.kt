package com.abounegm.sup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abounegm.sup.ui.theme.СУПBalanceTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureBackgroundUpdatesRegistered(applicationContext)
        setContent {
            СУПBalanceTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App() {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.padding(10.dp)) {
                Header()
            }
            DailyLimit()
            Box(modifier = Modifier.padding(10.dp)) {
                Balance()
            }
            Divider(thickness = 2.dp)
            HistorySection()
        }
    }

}

@Composable
fun CardNumber() {
    val cardNumberLength = 13
    val context = LocalContext.current
    val store = BalanceStore(context)
    val emptyCardNumber = "0".repeat(cardNumberLength)
    val cardNumber = store.getCardNumber.collectAsState(initial = emptyCardNumber)
    var editing by remember { mutableStateOf(false) }

    if (editing) {
        val cardNumberField = remember { mutableStateOf(cardNumber.value) }
        var validationError by remember { mutableStateOf<String?>(null) }

        fun validate(input: String): Boolean {
            validationError = if (input.length != cardNumberLength) {
                "The card number must be exactly $cardNumberLength digits"
            } else {
                null
            }
            return validationError == null
        }

        Row {
            TextField(
                modifier = Modifier.width(180.dp),
                placeholder = {
                    Text(CardMaskTransformation().filter(AnnotatedString(emptyCardNumber)).text)
                },
                value = cardNumberField.value,
                singleLine = true,
                onValueChange = { value ->
                    cardNumberField.value = value
                        .filter { it.isDigit() }
                        .take(cardNumberLength)
                    validationError = null
                },
                visualTransformation = CardMaskTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = validationError != null,
                supportingText = {
                    validationError?.let { Text(it) }
                }
            )
            IconButton(onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    store.saveCardNumber(cardNumberField.value)
                }
                if (validate(cardNumberField.value)) {
                    editing = false
                }
            }) {
                Icon(Icons.Outlined.Check, "Confirm")
            }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = CardMaskTransformation()
                    .filter(AnnotatedString(cardNumber.value
                        .ifBlank { emptyCardNumber }
                        .toString())
                    ).text,
            )
            IconButton(onClick = { editing = true }) {
                Icon(Icons.Outlined.Edit, "Edit")
            }
        }
    }
}

/* Taken from https://stackoverflow.com/a/69064274 */
class CardMaskTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // 0 000000 000000
        var out = ""
        for (i in text.text.indices) {
            out += text.text[i]
            if (i == 0 || i == 6) out += " "
        }

        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset == 0) return 0
                if (offset <= 7) return offset + 1
                return offset + 2
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 8) return offset - 1
                return offset - 2
            }
        }

        return TransformedText(AnnotatedString(out), numberOffsetTranslator)
    }
}

@Composable
fun Header() {
    val context = LocalContext.current
    val store = BalanceStore(context)
    var updating by remember { mutableStateOf(false) }
    var errorMsg: String? by remember { mutableStateOf(null) }
    val lastUpdated = store.getLastUpdated.collectAsState(initial = Date.from(Instant.EPOCH))
    val timeFormatter = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box {
            CardNumber()
        }
        Row {
            Text(
                text = stringResource(R.string.last_updated) + "\n${timeFormatter.format(lastUpdated.value)}",
                style = TextStyle(textAlign = TextAlign.End, fontSize = 15.sp),
                modifier = Modifier.padding(4.dp, 0.dp)
            )
            if (updating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                IconButton(
                    onClick = {
                        updating = true
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                store.updateValues()
                            } catch (e: Exception) {
                                println(e)
                                errorMsg = e.message
                            }
                            updating = false
                        }
                    },
                    modifier = Modifier.size(25.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, "Refresh")
                }
            }
        }
    }

    if (errorMsg != null) {
        AlertDialog(
            title = {
                Text(stringResource(R.string.error))
            },
            text = {
                Text(text = stringResource(R.string.error_message) + ":\n" + errorMsg)
            },
            onDismissRequest = {
                errorMsg = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        errorMsg = null
                    }
                ) {
                    Text("Ok")
                }
            },
        )
    }
}

@Composable
fun DailyLimit() {
    val context = LocalContext.current
    val store = BalanceStore(context)
    val limit = store.getLimit.collectAsState(initial = Limit.getDefaultInstance())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            modifier = Modifier.alignByBaseline(),
            text = DecimalFormat("#.##").format(limit.value?.remainingLimit),
            fontSize = 60.sp,
        )
        Text(
            modifier = Modifier.alignByBaseline(),
            text = " / ${DecimalFormat("#.##").format(limit.value?.totalLimit)} ₽",
            fontSize = 30.sp,
        )
    }

}

@Composable
fun Balance() {
    val context = LocalContext.current
    val store = BalanceStore(context)
    val balance = store.getBalance.collectAsState(initial = 0f)

    Row(
        Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.balance, DecimalFormat("#.##").format(balance.value)))
    }

}

@Composable
fun HistorySection() {
    val store = BalanceStore(LocalContext.current)
    val transactions = store.getHistory.map { it.transactionsList }
        .collectAsState(initial = listOf())

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.history),
            fontSize = 24.sp,
            style = TextStyle(textDecoration = TextDecoration.Underline)
        )
    }
    LazyColumn {
        items(transactions.value) {
            HistoryListItem(it)
        }
    }
}

@Composable
fun HistoryListItem(item: HistoryItem) {
    ListItem(
        leadingContent = { ItemIcon(item.type) },
        // Some names have repeating spaces in the middle, and it bothers me 😁
        headlineContent = { Text(item.name.replace("\\s+".toRegex(), " ")) },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${DecimalFormat("#.##").format(item.amount)} ₽",
                    fontSize = 16.sp,
                    style = TextStyle(
                        color = if (item.amount > 0) Color(
                            0,
                            128,
                            0
                        ) else Color.Unspecified
                    )
                )
                Text(
                    text = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(item.time.seconds * 1000))
                )
            }
        },
    )
}

@Composable
fun ItemIcon(type: TransactionType) {
    val resource = when (type) {
        TransactionType.FAST_FOOD -> R.drawable.fast_food
        TransactionType.RESTAURANT -> R.drawable.restaurant
        TransactionType.INCOMING -> R.drawable.replenishment
        TransactionType.REFUND -> R.drawable.refund
        TransactionType.GENERIC -> R.drawable.generic
        TransactionType.UNRECOGNIZED -> R.drawable.ic_launcher_foreground
    }
    val green = Color(129, 199, 137)
    val orange = Color(236, 87, 35)
    val bgColor = when (type) {
        TransactionType.FAST_FOOD -> green
        TransactionType.RESTAURANT -> green
        TransactionType.INCOMING -> orange
        TransactionType.REFUND -> orange
        TransactionType.GENERIC -> green
        TransactionType.UNRECOGNIZED -> Color.Black
    }
    Icon(
        painter = painterResource(resource),
        contentDescription = "History item logo",
        tint = Color.White,
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(bgColor)
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    СУПBalanceTheme {
        App()
    }
}