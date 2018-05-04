public class Chocolate extends Food {

  String eaten(Dog dog) {
    return "dog eats chocolate";
  }

  String eaten(Cat cat) {
    return "cat eats chocolate";
  }

  String eaten(Animal animal) {
    return "animal eats chocolate";
  }

}
