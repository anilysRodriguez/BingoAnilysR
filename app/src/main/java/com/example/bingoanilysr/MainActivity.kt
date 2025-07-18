package com.example.bingoanilysr

import android.annotation.SuppressLint
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bingoanilysr.ui.theme.BingoAnilysRTheme
import java.util.Locale
import kotlin.random.Random

data class Cell(val number: Int, val isMarked: Boolean = false)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    @SuppressLint("MutableCollectionMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        enableEdgeToEdge()

        setContent {
            var screen by remember { mutableStateOf<Screen>(Screen.Start) }
            var dimension by remember { mutableIntStateOf(5) }
            var uid by remember { mutableStateOf(generateUid()) }
            var card by remember { mutableStateOf(generateCard(dimension)) }

            BingoAnilysRTheme {
                when (screen) {
                    Screen.Start -> StartScreen(
                        dimension = dimension,
                        onDimensionChange = {
                            dimension = it.coerceAtLeast(3)
                        },
                        uid = uid,
                        onGenerate = {
                            card = generateCard(dimension)
                            screen = Screen.Game
                        }
                    )

                    Screen.Game -> BingoScreen(
                        card = card,
                        onCellClick = { r, c ->
                            val newCard = card.map { it.toMutableList() }.toMutableList()
                            val current = newCard[r][c]
                            newCard[r][c] = current.copy(isMarked = !current.isMarked)
                            card = newCard

                            if (checkBingo(newCard)) {
                                Toast.makeText(this@MainActivity, "¡BINGO!", Toast.LENGTH_LONG).show()
                                tts.speak("Bingo!", TextToSpeech.QUEUE_FLUSH, null, "bingoId")
                            }
                        },
                        onRegenerate = { card = generateCard(dimension) },
                        onBack = {
                            screen = Screen.Start
                            uid = generateUid()
                        }
                    )
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
private sealed class Screen {
    object Start : Screen()
    object Game : Screen()
}

private fun generateUid(): String =
    List(6) { if (Random.nextBoolean()) ('A'..'Z').random() else ('0'..'9').random() }
        .joinToString("")

private fun generateCard(dim: Int): MutableList<MutableList<Cell>> {
    val maxNumber = dim * dim * 2
    val pool = (1..maxNumber).shuffled().take(dim * dim)
    return pool.chunked(dim)
        .map { row -> row.map { Cell(it) }.toMutableList() }
        .toMutableList()
}

private fun checkBingo(card: List<List<Cell>>): Boolean {
    val n = card.size
    for (r in 0 until n) if (card[r].all { it.isMarked }) return true
    for (c in 0 until n) if ((0 until n).all { card[it][c].isMarked }) return true
    if ((0 until n).all { card[it][it].isMarked }) return true
    if ((0 until n).all { card[it][n - it - 1].isMarked }) return true
    return false
}

@Composable
private fun StartScreen(
    dimension: Int,
    onDimensionChange: (Int) -> Unit,
    uid: String,
    onGenerate: () -> Unit
) {
    var text by remember { mutableStateOf(dimension.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.bingo),
            contentDescription = "Imagen del Bot",
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it.filter { ch -> ch.isDigit() }
                text.toIntOrNull()?.let(onDimensionChange)
            },
            label = { Text("Dimensión del Bingo") },
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))
        Text("UID: $uid", fontWeight = FontWeight.Medium)

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGenerate,
            enabled = text.isNotBlank()
        ) { Text("Generar Bingo") }
    }
}

@Composable
fun BingoScreen(
    card: List<List<Cell>>,
    onCellClick: (Int, Int) -> Unit,
    onRegenerate: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Juego de Bingo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                text = "BINGO VIRTUAL",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(16.dp))

            BingoGrid(card, onCellClick)

            Spacer(Modifier.height(16.dp))

            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(24.dp))

            Button(onClick = onRegenerate) {
                Text("Regenerar Carta")
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = onBack) {
                Text("Atrás")
            }
        }
    }
}

@Composable
fun SmallTopAppBar(title: @Composable () -> Unit, navigationIcon: @Composable () -> Unit    ) {

}

@Composable
private fun BingoGrid(card: List<List<Cell>>, onCellClick: (Int, Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        card.forEachIndexed { r, row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                row.forEachIndexed { c, cell ->
                    BingoBall(cell) { onCellClick(r, c) }
                }
            }
        }
    }
}

@Composable
private fun BingoBall(cell: Cell, onClick: () -> Unit) {
    val borderColor = Color.Red
    val bgColor = if (cell.isMarked) Color(0xFFFFC1CC) else Color(0xFFB3E5FC)

    Box(
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .border(2.dp, Color(0xFFB39DDB), CircleShape) // borde lila claro
            .background(bgColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (cell.isMarked) "\uD83C\uDFB1" else cell.number.toString(),
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStart() {
    BingoAnilysRTheme {
      StartScreen(dimension = 5, onDimensionChange = {}, uid = "AEx01P") {}
    }
}