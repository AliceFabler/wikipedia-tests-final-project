package guru.qa.ui.utils;

import net.datafaker.Faker;

public class DataGenerator {

    Faker faker = new Faker();

    public String getRandomSearchValue() {
        return faker.country().name();
    }
}
