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
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abounegm.sup.ui.theme.СУПBalanceTheme

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
            Header()
            DailyLimit()
            Balance()
            Divider(thickness = 2.dp)
            History()
        }
    }

}

@Composable
fun Header() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box {
            Text(
                text = "Card number"
            )
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