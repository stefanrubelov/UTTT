package dk.easv.bll.bot;

import dk.easv.bll.field.Field;
import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;
import dk.easv.bll.move.Move;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GringoBot implements IBot {
    private static final String BOTNAME = "Gringo Bot!";
    private Random rand = new Random();
    private static final int WIN_SCORE = 100000;
    private static final int SIMULATION_DEPTH_LIMIT = 15;
    private static final int DEFENSIVE_PRIORITY = 2000;
    private static final int[][] POSITION_WEIGHTS = {
            {5, 1, 5, 1, 8, 1, 5, 1, 5},
            {1, 3, 1, 3, 8, 3, 1, 3, 1},
            {5, 1, 5, 1, 8, 1, 5, 1, 5},
            {1, 3, 1, 3, 8, 3, 1, 3, 1},
            {8, 8, 8, 8, 10, 8, 8, 8, 8},
            {1, 3, 1, 3, 8, 3, 1, 3, 1},
            {5, 1, 5, 1, 8, 1, 5, 1, 5},
            {1, 3, 1, 3, 8, 3, 1, 3, 1},
            {5, 1, 5, 1, 8, 1, 5, 1, 5}};

    private static final double EXPLORATION_CONSTANT = 1.414;
    private static final int BASE_SIMULATION_COUNT = 500;
    private String botId;
    private String opponentId;

    @Override
    public String getBotName() {
        return BOTNAME;
    }

    /**
     * Makes a turn. Implement this method to make your dk.easv.bll.bot do something.
     *
     * @param state the current dk.easv.bll.game state
     * @return The column where the turn was made.
     */
    @Override
    public IMove doMove(IGameState state) {
        List<IMove> moves = state.getField().getAvailableMoves();

        if (moves.isEmpty()) {
            return null;
        }

        if (botId == null && opponentId == null) {
            determinePlayerIds(state);
        }

        // First move strategy - always take center if available
        if (state.getMoveNumber() == 0) {
            IMove centerMove = getCenterCell(moves);
            if (centerMove != null) return centerMove;
        }

        // Check for immediate winning or blocking opportunities
        IMove immediateWin = findImmediateWinOrBlock(state, botId);
        if (immediateWin != null) return immediateWin;
        IMove immediateBlock = findImmediateWinOrBlock(state, opponentId);
        if (immediateBlock != null) return immediateBlock;

        // Early game strategy
        if (state.getMoveNumber() < 2) {
            // If center is taken, prioritize corners
            if (state.getField().getPlayerId(4, 4).equals(opponentId)) {
                IMove cornerMove = getCornerCell(state.getField().getAvailableMoves());
                if (cornerMove != null) return cornerMove;
            }
            // Avoid middle edges in early game
            return avoidMiddleEdges(state.getField().getAvailableMoves());
        }

        // Check for two-in-a-line patterns first
        IMove patternMove = findPatternMove(state);
        if (patternMove != null) {
            return patternMove;
        }

        // Then check for other tactical moves
        IMove tacticalMove = findTacticalMove(state);
        if (tacticalMove != null) {
            return tacticalMove;
        }

        return findBestMove(state);
    }

    private void determinePlayerIds(IGameState state) {
        if (state.getMoveNumber() == 0) {
            botId = "0";
            opponentId = "1";
        } else if (state.getMoveNumber() == 1) {
            botId = "1";
            opponentId = "0";
        }
    }

    private String getCurrentPlayer(IGameState state) {
        if (botId == null || opponentId == null) {
            determinePlayerIds(state);
        }

        return (state.getMoveNumber() % 2 == 0) ? "0" : "1";
    }

    private String getBotId(IGameState state) {
        if (botId == null || opponentId == null) {
            determinePlayerIds(state);
        }

        return botId;
    }

    private IMove getCell(List<IMove> moves, int[][] coordinates) {
        for (int[] coordinate : coordinates) {
            int x = coordinate[0];
            int y = coordinate[1];

            for (IMove move : moves) {
                if (move.getX() == x && move.getY() == y) {
                    return move;
                }
            }
        }

        return moves.get(rand.nextInt(moves.size()));
    }

    private IMove getCenterCell(List<IMove> moves) {
        // Prioritize the absolute center first
        for (IMove move : moves) {
            if (move.getX() == 4 && move.getY() == 4) {
                return move;
            }
        }

        // Then try other center positions
        int[][] centerCoordinates = {{1, 1}, {4, 1}, {7, 1}, {1, 4}, {7, 4}, {1, 7}, {4, 7}, {7, 7}};
        return getCell(moves, centerCoordinates);
    }

    private IMove getCornerCell(List<IMove> moves) {
        int[][] cornerCoordinates = {{0, 0}, {2, 0}, {0, 2}, {2, 2}, {3, 0}, {5, 0}, {3, 2}, {5, 2}, {6, 0}, {8, 0}, {6, 2}, {8, 2}, {0, 3}, {2, 3}, {0, 5}, {2, 5}, {3, 3}, {5, 3}, {3, 5}, {5, 5}, {6, 3}, {8, 3}, {6, 5}, {8, 5}, {0, 6}, {2, 6}, {0, 8}, {2, 8}, {3, 6}, {5, 6}, {3, 8}, {5, 8}, {6, 6}, {8, 6}, {6, 8}, {8, 8}};

        return getCell(moves, cornerCoordinates);
    }

    private IMove avoidMiddleEdges(List<IMove> moves) {
        // Middle edge coordinates to avoid
        int[][] middleEdges = {{1, 0}, {0, 1}, {2, 1}, {1, 2}};

        for (IMove move : moves) {
            boolean isMiddleEdge = false;
            for (int[] edge : middleEdges) {
                if (move.getX() % 3 == edge[0] && move.getY() % 3 == edge[1]) {
                    isMiddleEdge = true;
                    break;
                }
            }
            if (!isMiddleEdge) {
                return move;
            }
        }
        // If no other option, return first available move
        return moves.get(0);
    }

    private IMove findImmediateWinOrBlock(IGameState state, String playerId) {
        IField field = state.getField();
        List<IMove> availableMoves = field.getAvailableMoves();

        // Check each available move
        for (IMove move : availableMoves) {
            int microX = move.getX() / 3;
            int microY = move.getY() / 3;

            // Only check moves in active microboards
            if (field.getMacroboard()[microY][microX].equals(IField.AVAILABLE_FIELD)) {
                // Check if this move completes a line
                if (completesLine(field, move, playerId)) {
                    return move;
                }
            }
        }

        return null;
    }

    private boolean completesLine(IField field, IMove move, String playerId) {
        int x = move.getX();
        int y = move.getY();
        int microX = x / 3;
        int microY = y / 3;
        int cellX = x % 3;
        int cellY = y % 3;

        // Check row
        int rowMatches = 0;
        for (int i = 0; i < 3; i++) {
            int checkX = microX * 3 + i;
            if (i != cellX && field.getPlayerId(checkX, y).equals(playerId)) rowMatches++;
        }
        if (rowMatches == 2) return true;

        // Check column
        int colMatches = 0;
        for (int i = 0; i < 3; i++) {
            int checkY = microY * 3 + i;
            if (i != cellY && field.getPlayerId(x, checkY).equals(playerId)) colMatches++;
        }
        if (colMatches == 2) return true;

        // Check diagonals if the move is on a diagonal
        if ((cellX == cellY) || (cellX + cellY == 2)) {
            return checkDiagonals(field, move, playerId, microX, microY, cellX, cellY);
        }

        return false;
    }

    private boolean checkDiagonals(IField field, IMove move, String playerId, int microX, int microY, int cellX, int cellY) {
        int startX = microX * 3;
        int startY = microY * 3;

        // Main diagonal (top-left to bottom-right)
        if (cellX == cellY) {
            int matches = 0;
            for (int i = 0; i < 3; i++) {
                if (i != cellX && field.getPlayerId(startX + i, startY + i).equals(playerId)) matches++;
            }
            if (matches == 2) return true;
        }

        // Anti-diagonal (top-right to bottom-left)
        if (cellX + cellY == 2) {
            int matches = 0;
            for (int i = 0; i < 3; i++) {
                if (i != cellX && field.getPlayerId(startX + i, startY + (2 - i)).equals(playerId)) matches++;
            }
            if (matches == 2) return true;
        }

        return false;
    }

    private IMove findPatternMove(IGameState state) {
        // Early game strategy
        if (state.getMoveNumber() < 2) {
            // If center is taken, prioritize corners
            if (state.getField().getPlayerId(4, 4).equals(opponentId)) {
                IMove cornerMove = getCornerCell(state.getField().getAvailableMoves());
                if (cornerMove != null) return cornerMove;
            }
            // Avoid middle edges in early game
            return avoidMiddleEdges(state.getField().getAvailableMoves());
        }

        // First check if we can win in any microboard
        IMove winningPattern = findTwoInLinePattern(state, getBotId(state), true);
        if (winningPattern != null) {
            return winningPattern;
        }

        // Then check if we need to block opponent
        IMove blockingPattern = findTwoInLinePattern(state, opponentId, true);
        if (blockingPattern != null) {
            return blockingPattern;
        }

        // Look for strategic patterns
        return findTwoInLinePattern(state, getBotId(state), false);
    }

    private IMove findTwoInLinePattern(IGameState state, String playerId, boolean immediate) {
        IField field = state.getField();
        List<IMove> availableMoves = field.getAvailableMoves();
        if (availableMoves.isEmpty()) return null;

        for (int microX = 0; microX < 3; microX++) {
            for (int microY = 0; microY < 3; microY++) {
                if (!immediate || field.getMacroboard()[microY][microX].equals(IField.AVAILABLE_FIELD)) {
                    IMove move = checkMicroboardPatterns(field, microX, microY, playerId, availableMoves);
                    if (move != null) {
                        return move;
                    }
                }
            }
        }
        return null;
    }

    private IMove checkMicroboardPatterns(IField field, int microX, int microY, String playerId, List<IMove> availableMoves) {
        int startX = microX * 3;
        int startY = microY * 3;

        // Check rows
        for (int y = 0; y < 3; y++) {
            IMove move = checkLine(field, startX, startY + y, 1, 0, playerId, availableMoves);
            if (move != null) return move;
        }

        // Check columns
        for (int x = 0; x < 3; x++) {
            IMove move = checkLine(field, startX + x, startY, 0, 1, playerId, availableMoves);
            if (move != null) return move;
        }

        // Check diagonals
        IMove move = checkLine(field, startX, startY, 1, 1, playerId, availableMoves);
        if (move != null) return move;

        return checkLine(field, startX + 2, startY, -1, 1, playerId, availableMoves);
    }

    private IMove checkLine(IField field, int startX, int startY, int dx, int dy, String playerId, List<IMove> availableMoves) {
        String[] line = new String[3];
        int[] xCoords = new int[3];
        int[] yCoords = new int[3];

        // Get the line contents and coordinates
        for (int i = 0; i < 3; i++) {
            int x = startX + (dx * i);
            int y = startY + (dy * i);
            line[i] = field.getPlayerId(x, y);
            xCoords[i] = x;
            yCoords[i] = y;
        }

        // Count player's pieces and empty spaces
        int playerCount = 0;
        int emptyCount = 0;
        int emptyIndex = -1;

        for (int i = 0; i < 3; i++) {
            if (line[i].equals(playerId)) {
                playerCount++;
            } else if (line[i].equals(IField.EMPTY_FIELD)) {
                emptyCount++;
                emptyIndex = i;
            }
        }

        // If we found two pieces and one empty space
        if (playerCount == 2 && emptyCount == 1) {
            IMove potentialMove = new Move(xCoords[emptyIndex], yCoords[emptyIndex]);
            // Check if the move is actually available
            for (IMove availableMove : availableMoves) {
                if (moveEquals(availableMove, potentialMove)) {
                    return potentialMove;
                }
            }
        }

        return null;
    }

    private IMove findTacticalMove(IGameState state) {
        List<IMove> availableMoves = state.getField().getAvailableMoves();

        // First, check for winning moves
        for (IMove move : availableMoves) {
            if (isWinningMove(state, move, getBotId(state))) {
                return move;
            }
        }

        // Then, check for blocking opponent's winning moves
        for (IMove move : availableMoves) {
            if (isWinningMove(state, move, opponentId)) {
                return move;
            }
        }

        // Check for fork opportunities
        IMove forkMove = findForkMove(state);
        if (forkMove != null) {
            return forkMove;
        }

        return null;
    }

    private boolean isWinningMove(IGameState state, IMove move, String playerId) {
        GameState simulatedState = cloneGameState(state);
        applyMove(simulatedState, move);
        return hasWonInMicroboard(simulatedState.getField(), playerId, move.getX() / 3, move.getY() / 3);
    }

    private IMove findForkMove(IGameState state) {
        List<IMove> availableMoves = state.getField().getAvailableMoves();
        String currentPlayer = getCurrentPlayer(state);

        for (IMove move : availableMoves) {
            GameState simulatedState = cloneGameState(state);
            applyMove(simulatedState, move);

            if (countWinningThreats(simulatedState, currentPlayer) >= 2) {
                return move;
            }
        }
        return null;
    }

    private int countWinningThreats(GameState state, String playerId) {
        int threats = 0;
        List<IMove> availableMoves = state.getField().getAvailableMoves();

        for (IMove move : availableMoves) {
            if (isWinningMove(state, move, playerId)) {
                threats++;
            }
        }
        return threats;
    }

    private int evaluatePosition(IGameState state, String playerId) {
        int score = 0;
        IField field = state.getField();
        String[][] macroboard = field.getMacroboard();
        String opponentId = playerId.equals("0") ? "1" : "0";

        // Prioritize control of center and corner microboards
        if (macroboard[1][1].equals(playerId)) score += 500;
        if (macroboard[0][0].equals(playerId) || macroboard[0][2].equals(playerId) ||
            macroboard[2][0].equals(playerId) || macroboard[2][2].equals(playerId)) {
            score += 300;
        }

        // Check for wins in microboards
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (hasWonInMicroboard(field, playerId, i, j)) {
                    score += WIN_SCORE;
                }
                // Add defensive scoring
                if (needsBlocking(field, i, j, opponentId)) {
                    score += DEFENSIVE_PRIORITY;
                }
            }
        }

        // Add positional score
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (field.getPlayerId(i, j).equals(playerId)) {
                    score += POSITION_WEIGHTS[i][j];
                }
            }
        }

        return score;
    }

    private boolean needsBlocking(IField field, int microX, int microY, String opponentI) {
        int startX = microX * 3;
        int startY = microY * 3;
        int opponentCount = 0;

        // Check all lines in microboard
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (field.getPlayerId((startX + j), (startY + i)).equals(opponentI)) {
                    opponentCount++;
                }

            }
            if (opponentCount == 2) return true;
            opponentCount = 0;
        }
        return false;
    }

    /**
     * Applies Monte Carlo Tree Search to find the best move
     */
    private IMove findBestMove(IGameState state) {
        // Create root node
        Node rootNode = new Node(null, null);
        IField field = state.getField();

        // Run simulations
        for (int i = 0; i < getSimulationCount(state); i++) {
            // Clone the current game state for simulation
            GameState clonedState = cloneGameState(state);

            // Selection and expansion phases - find the node to expand
            Node selectedNode = selection(rootNode, clonedState);

            // Simulation phase - play random moves until game over
            String result = simulation(selectedNode, clonedState);

            // Backpropagation phase - update nodes with results
            backpropagation(selectedNode, result);
        }

        // Choose the best move based on most visits
        return getBestMoveFromStats(rootNode, field.getAvailableMoves());
    }

    private List<IMove> getWeightedMoves(IField field) {
        List<IMove> moves = field.getAvailableMoves();
        List<IMove> weightedMoves = new ArrayList<>();

        for (IMove move : moves) {
            int weight = POSITION_WEIGHTS[move.getX()][move.getY()];
            // Add the move multiple times based on its weight
            for (int i = 0; i < weight; i++) {
                weightedMoves.add(move);
            }
        }
        return weightedMoves;
    }

    private int getSimulationCount(IGameState state) {
        int timeLeft = state.getTimePerMove();
        return Math.min(BASE_SIMULATION_COUNT, timeLeft / 2);
    }

    private Node selection(Node node, GameState state) {
        while (!isTerminal(state) && isFullyExpanded(node, state)) {
            node = selectBestChild(node, EXPLORATION_CONSTANT);
            applyMove(state, node.move);
        }

        // If node is not fully expanded, expand it
        if (!isTerminal(state) && !isFullyExpanded(node, state)) {
            return expand(node, state);
        }

        return node;
    }

    private Node expand(Node node, GameState state) {
        List<IMove> availableMoves = state.getField().getAvailableMoves();
        List<IMove> triedMoves = new ArrayList<>();

        // Get list of moves already tried from this node
        for (Node child : node.children) {
            triedMoves.add(child.move);
        }

        // Find a move that hasn't been tried yet
        for (IMove move : availableMoves) {
            boolean moveTried = false;
            for (IMove triedMove : triedMoves) {
                if (moveEquals(move, triedMove)) {
                    moveTried = true;
                    break;
                }
            }

            if (!moveTried) {
                // Create a new node with this move
                Node newNode = new Node(node, move);
                node.children.add(newNode);

                // Apply the move
                applyMove(state, move);

                return newNode;
            }
        }

        // Should not reach here if isFullyExpanded was checked properly
        return node;
    }

    private String simulation(Node node, GameState state) {
        int depth = 0;
        // Simulate until terminal state or depth limit is reached
        while (!isTerminal(state) && depth < SIMULATION_DEPTH_LIMIT) {
            List<IMove> availableMoves = state.getField().getAvailableMoves();
            if (availableMoves.isEmpty()) {
                break;
            }

            // Use weighted move selection
            IMove randomMove;
            if (depth < 3) {
                // Early moves: prefer strategic positions
                List<IMove> weightedMoves = getWeightedMoves(state.getField());
                randomMove = weightedMoves.get(rand.nextInt(weightedMoves.size()));
            } else {
                // Later moves: mix of random and tactical
                randomMove = availableMoves.get(rand.nextInt(availableMoves.size()));
            }
            applyMove(state, randomMove);
            depth++;
        }

        // Determine the result of the simulation
        return evaluateState(state);
    }

    private void backpropagation(Node node, String result) {
        while (node != null) {
            node.visits++;

            if (result.equals("win")) {
                node.wins++;
            } else if (result.equals("draw")) {
                node.wins += 0.5; // Count draws as half wins
            }

            node = node.parent;
        }
    }

    private boolean isTerminal(GameState state) {
        return state.getField().getAvailableMoves().isEmpty() || hasWon(state.getField(), botId) || hasWon(state.getField(), opponentId);
    }

    private boolean isFullyExpanded(Node node, GameState state) {
        List<IMove> availableMoves = state.getField().getAvailableMoves();

        // If no available moves, node is fully expanded
        if (availableMoves.isEmpty()) {
            return true;
        }

        // Check if all available moves have been tried
        for (IMove move : availableMoves) {
            boolean moveTried = false;
            for (Node child : node.children) {
                if (moveEquals(move, child.move)) {
                    moveTried = true;
                    break;
                }
            }

            if (!moveTried) {
                return false; // Found an untried move
            }
        }

        return true; // All moves have been tried
    }

    private Node selectBestChild(Node node, double explorationParam) {
        double bestScore = Double.NEGATIVE_INFINITY;
        Node bestChild = null;
        double parentVisits = Math.log(node.visits);

        for (Node child : node.children) {
            // Enhanced UCT scoring with position weights
            double exploitation = child.wins / (child.visits + Double.MIN_VALUE);
            double exploration = explorationParam * Math.sqrt(parentVisits / (child.visits + Double.MIN_VALUE));

            // Add position weight bonus
            double positionBonus = POSITION_WEIGHTS[child.move.getX()][child.move.getY()] / 10.0;
            double score = exploitation + exploration + positionBonus;

            if (score > bestScore) {
                bestScore = score;
                bestChild = child;
            }
        }

        return bestChild;
    }

    private IMove getBestMoveFromStats(Node rootNode, List<IMove> availableMoves) {
        int mostVisits = -1;
        Node bestChild = null;

        for (Node child : rootNode.children) {
            if (child.visits > mostVisits) {
                mostVisits = child.visits;
                bestChild = child;
            }
        }

        if (bestChild != null) {
            return bestChild.move;
        }

        // Fallback to random move if no statistics available
        return availableMoves.get(rand.nextInt(availableMoves.size()));
    }

    private boolean moveEquals(IMove move1, IMove move2) {
        return move1.getX() == move2.getX() && move1.getY() == move2.getY();
    }

    private void applyMove(GameState state, IMove move) {
        if (move == null) return;

        IField field = state.getField();
        String[][] board = field.getBoard();

        // Determine the player (alternating X and O)
        String player = getCurrentPlayer(state);

        // Apply move
        board[move.getY()][move.getX()] = player;

        // Update the macroboard (active microboards)
        updateMacroboard(field, move);

        // Increment move number
        state.setMoveNumber(state.getMoveNumber() + 1);
    }

    private void updateMacroboard(IField field, IMove move) {
        String[][] macroboard = field.getMacroboard();
        String[][] board = field.getBoard();

        // Calculate which microboard coordinates the move points to
        int nextMicroboardX = move.getX() % 3;
        int nextMicroboardY = move.getY() % 3;

        // Calculate the microboard that this move was in
        int currentMicroboardX = move.getX() / 3;
        int currentMicroboardY = move.getY() / 3;

        // Check if the current microboard is won
        String microboardStatus = checkMicroboardStatus(board, currentMicroboardX, currentMicroboardY);
        if (!microboardStatus.equals("ongoing")) {
            macroboard[currentMicroboardY][currentMicroboardX] = microboardStatus;
        }

        // Reset all microboards to inactive
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (macroboard[y][x].equals(IField.AVAILABLE_FIELD)) {
                    macroboard[y][x] = IField.EMPTY_FIELD;
                }
            }
        }

        // Set the next microboard as active, or all if the target is already won
        if (macroboard[nextMicroboardY][nextMicroboardX].equals(IField.EMPTY_FIELD)) {
            macroboard[nextMicroboardY][nextMicroboardX] = IField.AVAILABLE_FIELD;
        } else {
            // The target microboard is full or won, make all non-won boards active
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    if (macroboard[y][x].equals(IField.EMPTY_FIELD)) {
                        macroboard[y][x] = IField.AVAILABLE_FIELD;
                    }
                }
            }
        }
    }

    private String checkMicroboardStatus(String[][] board, int microX, int microY) {
        // Get positions within this microboard
        String[] positions = new String[9];
        int index = 0;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                positions[index++] = board[microY * 3 + y][microX * 3 + x];
            }
        }

        // Check for winner
        // Rows
        for (int i = 0; i < 3; i++) {
            if (!positions[i * 3].equals(IField.EMPTY_FIELD) && positions[i * 3].equals(positions[i * 3 + 1]) && positions[i * 3].equals(positions[i * 3 + 2])) {
                return positions[i * 3];
            }
        }

        // Columns
        for (int i = 0; i < 3; i++) {
            if (!positions[i].equals(IField.EMPTY_FIELD) && positions[i].equals(positions[i + 3]) && positions[i].equals(positions[i + 6])) {
                return positions[i];
            }
        }

        // Diagonals
        if (!positions[0].equals(IField.EMPTY_FIELD) && positions[0].equals(positions[4]) && positions[0].equals(positions[8])) {
            return positions[0];
        }

        if (!positions[2].equals(IField.EMPTY_FIELD) && positions[2].equals(positions[4]) && positions[2].equals(positions[6])) {
            return positions[2];
        }

        // Check for draw (full board)
        boolean isFull = true;
        for (String pos : positions) {
            if (pos.equals(IField.EMPTY_FIELD)) {
                isFull = false;
                break;
            }
        }

        return isFull ? "draw" : "ongoing";
    }

    private boolean hasWon(IField field, String player) {
        for (int macroRow = 0; macroRow < 3; macroRow++) {
            for (int macroCol = 0; macroCol < 3; macroCol++) {
                if (hasWonInMacroboard(field, player, macroRow, macroCol)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasWonInMacroboard(IField field, String player, int macroRow, int macroCol) {
        // Calculate the starting indices for this macroboard
        int startRow = macroRow * 3;
        int startCol = macroCol * 3;

        // Check rows of the 3x3 subgrid
        for (int i = 0; i < 3; i++) {
            if (field.getPlayerId((int) (startCol), (int) (startRow + i)).equals(player) && field.getPlayerId((int) (startCol + 1), (int) (startRow + i)).equals(player) && field.getPlayerId((int) (startCol + 2), (int) (startRow + i)).equals(player)) {
                return true;
            }
        }

        // Check columns of the 3x3 subgrid
        for (int i = 0; i < 3; i++) {
            if (field.getPlayerId((int) (startCol + i), (int) (startRow)).equals(player) && field.getPlayerId((int) (startCol + i), (int) (startRow + 1)).equals(player) && field.getPlayerId((int) (startCol + i), (int) (startRow + 2)).equals(player)) {
                return true;
            }
        }

        // Check diagonals of the 3x3 subgrid
        // Top-left to bottom-right diagonal
        if (field.getPlayerId((int) (startCol), (int) (startRow)).equals(player) && field.getPlayerId((int) (startCol + 1), (int) (startRow + 1)).equals(player) && field.getPlayerId((int) (startCol + 2), (int) (startRow + 2)).equals(player)) {
            return true;
        }

        // Top-right to bottom-left diagonal
        if (field.getPlayerId((int) (startCol + 2), (int) (startRow)).equals(player) && field.getPlayerId((int) (startCol + 1), (int) (startRow + 1)).equals(player) && field.getPlayerId((int) (startCol), (int) (startRow + 2)).equals(player)) {
            return true;
        }

        return false;
    }

    private boolean hasWonInMicroboard(IField field, String player, int microX, int microY) {
        // Calculate the starting indices for this microboard
        int startRow = microY * 3;
        int startCol = microX * 3;

        // Check rows of the 3x3 subgrid
        for (int i = 0; i < 3; i++) {
            if (field.getPlayerId((int) (startCol), (int) (startRow + i)).equals(player) && field.getPlayerId((int) (startCol + 1), (int) (startRow + i)).equals(player) && field.getPlayerId((int) (startCol + 2), (int) (startRow + i)).equals(player)) {
                return true;
            }
        }

        // Check columns of the 3x3 subgrid
        for (int i = 0; i < 3; i++) {
            if (field.getPlayerId((int) (startCol + i), (int) (startRow)).equals(player) && field.getPlayerId((int) (startCol + i), (int) (startRow + 1)).equals(player) && field.getPlayerId((int) (startCol + i), (int) (startRow + 2)).equals(player)) {
                return true;
            }
        }

        // Check diagonals of the 3x3 subgrid
        // Top-left to bottom-right diagonal
        if (field.getPlayerId((int) (startCol), (int) (startRow)).equals(player) && field.getPlayerId((int) (startCol + 1), (int) (startRow + 1)).equals(player) && field.getPlayerId((int) (startCol + 2), (int) (startRow + 2)).equals(player)) {
            return true;
        }

        // Top-right to bottom-left diagonal
        if (field.getPlayerId((int) (startCol + 2), (int) (startRow)).equals(player) && field.getPlayerId((int) (startCol + 1), (int) (startRow + 1)).equals(player) && field.getPlayerId((int) (startCol), (int) (startRow + 2)).equals(player)) {
            return true;
        }

        return false;
    }

    private String evaluateState(GameState state) {
        IField field = state.getField();

        if (hasWon(field, botId)) { // Bot wins
            return "win";
        }

        if (hasWon(field, opponentId)) { // Opponent wins
            return "loss";
        }

        boolean allResolved = true;
        String[][] macroboard = field.getMacroboard();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (macroboard[y][x].equals(IField.EMPTY_FIELD) || macroboard[y][x].equals(IField.AVAILABLE_FIELD)) {
                    allResolved = false;
                    break;
                }
            }
            if (!allResolved) break;
        }

        if (allResolved) {
            return "draw";
        }

        return "ongoing";
    }

    private GameState cloneGameState(IGameState state) {
        try {
            GameState clonedState = new GameState();

            Field clonedField = new Field();

            String[][] originalBoard = state.getField().getBoard();
            String[][] clonedBoard = new String[9][9];
            for (int y = 0; y < 9; y++) {
                for (int x = 0; x < 9; x++) {
                    clonedBoard[y][x] = originalBoard[y][x];
                }
            }
            clonedField.setBoard(clonedBoard);

            String[][] originalMacroboard = state.getField().getMacroboard();
            String[][] clonedMacroboard = new String[3][3];
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    clonedMacroboard[y][x] = originalMacroboard[y][x];
                }
            }
            clonedField.setMacroboard(clonedMacroboard);

            java.lang.reflect.Field fieldField = GameState.class.getDeclaredField("field");
            fieldField.setAccessible(true);
            fieldField.set(clonedState, clonedField);

            clonedState.setMoveNumber(state.getMoveNumber());
            clonedState.setRoundNumber(state.getRoundNumber());
            clonedState.setTimePerMove(state.getTimePerMove());

            return clonedState;
        } catch (Exception e) {
            GameState fallbackState = new GameState();
            fallbackState.setMoveNumber(state.getMoveNumber());
            fallbackState.setRoundNumber(state.getRoundNumber());
            fallbackState.setTimePerMove(state.getTimePerMove());
            return fallbackState;
        }
    }

    private class Node {
        private IMove move;
        private Node parent;
        private List<Node> children;
        private int visits;
        private double wins; // Win count (1 for win, 0.5 for draw, 0 for loss)

        public Node(Node parent, IMove move) {
            this.parent = parent;
            this.move = move;
            this.children = new ArrayList<>();
            this.visits = 0;
            this.wins = 0;
        }
    }
}
