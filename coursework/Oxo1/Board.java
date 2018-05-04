class Board {

  private int row = 3;
  private int col = 3;
  private Player[][] state = new Player[row][col];
  private Player player;

  Board(Player p) {
    player = p;
    for (int x = 0; x < row; x++) {
      for (int y = 0; y < col; y++) {
        state[x][y] = Player.None;
  }}}

  // Takes in string of form '<row><col>'
  public Position position (String p) {
    if(p.length() != 2) return null;
    int r = p.charAt(0) - 'a';
    int c = p.charAt(1) - '1';
    if (r > 3 || c > 3 || state[r][c] != Player.None) return null;
    return new Position(r, c);
  }

  public void move(Position p) {
    state[p.row()][p.col()] = player;
    player = player.other();
  }

  public Player winner() {
    if (checkRow() != Player.None) return checkRow();
    if (checkCol() != Player.None) return checkCol();
    if (checkRightDiag() != Player.None) return checkRightDiag();
    if (checkLeftDiag() != Player.None) return checkLeftDiag();
    if (blanks().length == 0) return Player.Both;
    return Player.None;
  }

  private Player checkRow() {
    for (int r = 0; r < row; r++) {
      boolean win = true;
      Player cur = state[r][0];
      for (int c = 0; c < col; c++) {
        if (cur != state[r][c] || state[r][c] == Player.None) win = false;
      }
      if (win) {
        return cur;
      }
    }
    return Player.None;
  }

  private Player checkCol() {
    for (int c = 0; c < col; c++) {
      boolean win = true;
      Player cur = state[0][c];
      for (int r = 0; r < row; r++) {
        if (cur != state[r][c] || state[r][c] == Player.None) win = false;
      }
      if (win) {
        return cur;
      }
    }
    return Player.None;
  }

  // This will need to be improved for non-square boards
  /* Looking for
      X | - | -
      - | X | -
      - | - | x
  */
  private Player checkRightDiag() {
    Player cur = state[0][0];
    if (cur == Player.None) return Player.None;
    for (int d = 0; d < row; d++) {
      if (state[d][d] != cur) return Player.None;
    }
    return cur;
  }

  // This will need to be improved for non-square boards
  /* Looking for
      - | - | X
      - | X | -
      X | - | -
  */
  private Player checkLeftDiag() {
    Player cur = state[0][col-1];
    if (cur == Player.None) return Player.None;
    for (int d = 0; d < col; d++){
      if (state[d][row-(d+1)] != cur) return Player.None;
    }
    return cur;
  }

  public Position[] blanks() {
    Position[] blank = new Position[row*col];
    int count = 0;

    for (int r = 0; r < row; r++) {
      for (int c = 0; c < col; c++) {
        if (state[r][c] == Player.None) {
          blank[count] = new Position(r,c);
          count++;
    }}}

    Position[] re = new Position[count];
    for (int k = 0; k < count; k++) {
      re[k] = blank[k];
    }

    return re;
  }

  //".XO\nXO.\n.X.\n"
  public String toString() {
    String b = "";
    for (int r = 0; r < row; r++) {
      for (int c = 0; c < col; c++) {
        if (state[r][c] == Player.None) b = b + ".";
        else if (state[r][c] == Player.X) b = b + "X";
        else if (state[r][c] == Player.O) b = b + "O";
      }
      b = b + "\n";
    }
    return b;
  }

}
