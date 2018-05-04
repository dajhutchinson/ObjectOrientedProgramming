public class Fruit extends Food {

  String eaten(Dog dog) {
    return "dog eats fruit";
  }

  String eaten(Cat cat) {
    return "cat eats fruit";
  }

  String eaten(Animal animal) {
    return "animal eats fruit";
  }
}
