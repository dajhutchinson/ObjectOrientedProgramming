public class Animal {

  public String eat(Food other) {
    return other.eaten(this);
  }

  public String eat(Chocolate other) {
    return other.eaten(this);
  }

  public String eat(Fruit other) {
    return other.eaten(this);
  }

}
