import java.util.*

data class Cell(val player: Int, val size: Int) {
    fun update() =
            copy(size = if (size >= 0) size + 1 else 0)

    fun possibleActions(x: Int, y: Int, size: Int, player: Int): List<Action> {
        if (player != this.player || this.size <= 1) {
            return emptyList()
        }
        val possible = arrayListOf<Action>()
        if (x > 0) possible.add(Action(Dir.Left, Position(x, y)))
        if (y > 0) possible.add(Action(Dir.Up, Position(x, y)))
        if (x < size - 1) possible.add(Action(Dir.Right, Position(x, y)))
        if (y < size - 1) possible.add(Action(Dir.Down, Position(x, y)))
        return possible
    }
}

enum class Dir {
    None,
    Up,
    Right,
    Down,
    Left;

    fun toPos(): Position =
            when (this) {
                None -> Position(0, 0)
                Up -> Position(0, -1)
                Right -> Position(1, 0)
                Down -> Position(0, 1)
                Left -> Position(-1, 0)
            }
}

data class Position(val x: Int, val y: Int)

data class Action(val dir: Dir, val position: Position)

fun nextLevel(board: Array<Array<Cell>>) {
    for (x in board.indices) {
        for (y in board[x].indices) {
            board[x][y] = board[x][y].update()
        }
    }
}

fun evalCell(cell: Cell, player: Int, board: Array<Array<Cell>>, position: Position): Double {
    val playerFactor = when (cell.player) {
        0 -> 0
        player -> 1
        else -> -1
    }
    if (playerFactor == 0) return 0.0

    var dist = board.size
    for (x in board.indices) {
        for (y in board[x].indices) {
            // promote closeness to open or cells from others for defense / attack
            if (cell.player != board[x][y].player) {
                dist = Math.min(dist, Math.abs(x - position.x) + Math.abs(y - position.y))
            }
        }
    }
    val ownBonus = 10.0
    val closeToNewBonus = if (cell.player == player) 1.0 else 0.0
    return playerFactor * ((ownBonus + cell.size) - dist * closeToNewBonus)
}

fun moveCell(cell: Cell, other: Cell): Pair<Cell, Cell> {
    val isBigger = cell.size - 1 >= other.size || cell.player == other.player

    val isOwn = cell.player == other.player
    val otherNewCell = when (other.player) {
        0 -> Cell(size = cell.size - 1, player = cell.player)
        else -> Cell(size = if (isOwn) other.size + cell.size - 1 else if (isBigger) cell.size - other.size - 1 else other.size - cell.size + 1, player =
        if (isOwn) cell.player else if (isBigger) cell.player else other.player)
    }

    return Pair(cell.copy(size = 1), otherNewCell)
}

fun evalAction(action: Action, cell: Cell, board: Array<Array<Cell>>, x: Int, y: Int): Double {
    val pos = action.dir.toPos()
    val currentPos = Position(x, y)
    val other = board[x + pos.x][y + pos.y]

    val (newCell, otherNewCell) = moveCell(cell, other)
    return (evalCell(newCell, cell.player, board, currentPos) - evalCell(cell, cell.player, board, currentPos)) + (evalCell(otherNewCell, cell.player, board, currentPos) - evalCell(other, cell.player, board, currentPos))

}

fun doActions(actions: List<Action>, board: Array<Array<Cell>>) {
    actions.forEach {
        val pos = it.dir.toPos()
        val cell = board[it.position.x][it.position.y]
        val other = board[it.position.x + pos.x][it.position.y + pos.y]

        val (newCell, otherNewCell) = moveCell(cell, other)
        board[it.position.x][it.position.y] = newCell
        board[it.position.x + pos.x][it.position.y + pos.y] = otherNewCell
    }
}

fun evaluate(board: Array<Array<Cell>>, player: Int): Double =
        board.mapIndexed { x, it -> it.mapIndexed({ y, it -> evalCell(it, player, board, Position(x, y)) }).sum() }.sum()

fun actions(board: Array<Array<Cell>>, player: Int): List<Action> {
    val newBoard = board.copyOf()
    val actions = arrayListOf<Action>()

    for (x in newBoard.indices) {
        for (y in newBoard[x].indices) {
            val cell = newBoard[x][y]
            val action = cell.possibleActions(x, y, newBoard.size, player).maxBy { evalAction(it, cell, board, x, y) }
            if (action is Action) {
                actions.add(action)
                doActions(listOf(action), newBoard)
            }
        }
    }
    return actions
}


fun main(args: Array<String>) {
    val n = 10

    val board = Array(n, { Array(size = n, init = { Cell(0, 0) }) })

    board[0][5] = Cell(1, 1)
    board[9][5] = Cell(2, 1)

    var player = 1

    while (true) {
        player = if (player == 1) 2 else 1

        val actions = actions(board, player)
        doActions(actions, board)
        nextLevel(board)
        println(actions(board, player))
        println(evaluate(board, 1))
        println((evaluate(board, 2)))

        for (x in board.indices) {
            for (y in board[x].indices) {
                when(board[x][y].player) {
                    0 -> print(' ')
                    1 -> {print('X'); }
                    2 -> {print('O');}
                }
            }
            println()
        }
        Thread.sleep(1000)
    }
}