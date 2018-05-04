public class Cat extends Animal {

  @Override
  public String eat(Food other) {
    return other.eaten(this);
  }

  @Override
  public String eat(Chocolate other) {
    return other.eaten(this);
  }

  @Override
  public String eat(Fruit other) {
    return other.eaten(this);
  }

}
