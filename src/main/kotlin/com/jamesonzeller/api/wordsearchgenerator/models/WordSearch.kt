package com.jamesonzeller.api.wordsearchgenerator.models

import com.jamesonzeller.api.wordsearchgenerator.utils.RandomWords
import org.w3c.dom.ranges.RangeException
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

class WordSearch(
    inputWords: List<String>?,
) {
    data class Direction(val xStep: Int, val yStep: Int)
    data class PlacementInformation(val word: String,
                                    val coords: Pair<Int, Int>,
                                    val direction: Direction
    )

    final var words: List<String> = inputWords ?: RandomWords.randomWords()
    private val wordsCopy: MutableList<String> = words.toMutableList() // Working words list

    private val size: Int = computeGridSize()

    private val grid: MutableList<MutableList<String>> =
        MutableList(size) { MutableList(size) { "_" } }

    private val kDirections: List<Direction> = listOf(
        Direction(0, 1),
        Direction(1, 0),
        Direction(1, 1),
        Direction(1, -1),
        Direction(0,-1),
        Direction(-1,0),
        Direction(-1,-1),
        Direction(-1,1)
    )

    private fun computeGridSize(): Int {
        val totalCharacters = words.sumOf { word -> word.length }
        val averageWordLength = totalCharacters / words.size
        val maxWordLength = words.maxOf { word -> word.length }

        // Calculate initial grid size based on number of words and average word length
        var gridSize = ceil(sqrt(words.size.toDouble() * averageWordLength)).toInt()

        gridSize = max(gridSize, maxWordLength)

        gridSize *= 2
        return gridSize
    }

    private fun getRandomCoords(): Pair<Int, Int> {
        return Pair(Random.nextInt(0, size), Random.nextInt(0, size))
    }

    private fun getValidDirection(word: String, coords: Pair<Int, Int>): Direction? {
        val (xPos, yPos) = coords
        val shuffleDirection = kDirections.shuffled()
        var validDirection = true
        for (direction in shuffleDirection) {
            for (i in word.indices) {
                val yOffset = i * direction.yStep
                val xOffset = i * direction.xStep

                val currY = yPos + yOffset
                val currX = xPos + xOffset

                try {
                    // Checks if current location for word is blank or matches current character, if neither of those apply then direction is invalid.
                    if (grid[currY][currX] != "_" && grid[currY][currX] != word[i].toString()) {
                        validDirection = false
                        break
                    }
                } catch (e: IndexOutOfBoundsException) {
                    validDirection = false
                    break
                }
            }

            if (validDirection) {
                return direction
            }
        }
        return null
    }

    private fun placeWord(info: PlacementInformation): Unit {
        val (xPos, yPos) = info.coords
        val dir = info.direction
        val word = info.word

        for (i in word.indices) {
            val yOffset = i * dir.yStep
            val xOffset = i * dir.xStep

            val currY = yPos + yOffset
            val currX = xPos + xOffset

            grid[currY][currX] = word[i].toString()
        }

        return
    }

    private fun fillBlanksIn(): Unit {
        val uppercaseLetters = ('A'..'Z').toList()

        for (row in grid.indices) {
            for (col in grid[row].indices) {
                if (grid[row][col] == "_") {
                    grid[row][col] = uppercaseLetters.random().toString()
                }
            }
        }

        return
    }

    fun generateWordSearch(): List<List<String>> {
        while (wordsCopy.isNotEmpty()) {
            // Get random coords, current word, and random direction
            val coords = getRandomCoords()
            val word = wordsCopy[0].uppercase()

            // getValidDirection returns null if there is not a valid direction for that word at that coordinate
            // So if not direction then we continue, and we get new random coordinates and try again to get a valid direction for it
            // This will continue until the word has a valid direction and coordinate combo when it will then pass on and be placed in the grid
            val direction = getValidDirection(word, coords) ?: continue

            // Place word on board and remove from wordsCopy
            val placementInformation = PlacementInformation(word, coords, direction)
            placeWord(placementInformation)

            wordsCopy.removeAt(0)
        }

        // After all words are inserted, fill in blanks with random characters
        fillBlanksIn()
        return grid
    }
}