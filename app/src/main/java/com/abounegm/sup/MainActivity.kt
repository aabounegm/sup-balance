package com.abounegm.sup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abounegm.sup.ui.theme.СУПBalanceTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            History()
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
                onValueChange = {
                    cardNumberField.value = it
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
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box {
            CardNumber()
        }
        Box {
            Text(
                text = "Last updated:"
            )
        }

    }
}

@Composable
fun DailyLimit() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            modifier = Modifier.alignByBaseline(),
            text = "750",
            fontSize = 60.sp,
        )
        Text(
            modifier = Modifier.alignByBaseline(),
            text = " / 750 ₽",
            fontSize = 30.sp,
        )
    }

}

@Composable
fun Balance() {
    Row(
        Modifier.fillMaxWidth(),
    ) {
        Text("Balance: ")
        Text(
            text = "9000 ₽"
        )
    }

}

@Composable
fun History() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text("History", fontSize = 24.sp)
    }
    ListItem(
        leadingContent = {
            Text("icon")
        },
        headlineContent = {
            Text("shop name")
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text("amount", fontSize = 16.sp)
                Text("time")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    СУПBalanceTheme {
        App()
    }
}