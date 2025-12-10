package com.aicodementor.config;

import com.aicodementor.entity.Exercise;
import com.aicodementor.entity.User;
import com.aicodementor.repository.ExerciseRepository;
import com.aicodementor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Initialize database with test data for sauvegarder and publier functionality testing
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Initializing test data...");

        // Create or get teacher user
        User teacher = userRepository.findByUsername("teacher")
                .orElseGet(() -> {
                    User newTeacher = new User();
                    newTeacher.setUsername("teacher");
                    newTeacher.setEmail("teacher@demo.com");
                    newTeacher.setPassword("demo123");
                    newTeacher.setFullName("Prof. Demo");
                    newTeacher.setRole(User.UserRole.TEACHER);
                    newTeacher.setCreatedAt(LocalDateTime.now());
                    newTeacher.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(newTeacher);
                });

        logger.info("Teacher user ready: {}", teacher.getUsername());

        // Create test exercises if they don't exist
        if (exerciseRepository.count() == 0) {
            createTestExercises(teacher);
            logger.info("Test exercises created successfully");
        } else {
            logger.info("Exercises already exist, skipping test data creation");
        }
    }

    private void createTestExercises(User teacher) {
        // Exercise 1: Sauvegardé (not published)
        Exercise exercise1 = new Exercise();
        exercise1.setTitle("Somme des éléments pairs d'un tableau");
        exercise1.setDescription("Implémenter une fonction qui calcule la somme des éléments pairs d'un tableau d'entiers");
        exercise1.setTopic("Tableaux");
        exercise1.setDifficulty(Exercise.DifficultyLevel.L1);
        exercise1.setProblemStatement(
            "Écrivez une fonction qui prend un tableau d'entiers en paramètre et retourne la somme de tous les éléments pairs.\n\n" +
            "Exemple:\n" +
            "Input: [1, 2, 3, 4, 5, 6]\n" +
            "Output: 12 (2 + 4 + 6 = 12)"
        );
        exercise1.setStarterCode(
            "public class ArraySum {\n" +
            "    public static int sumEven(int[] array) {\n" +
            "        // TODO: Implement your solution here\n" +
            "        return 0;\n" +
            "    }\n" +
            "}"
        );
        exercise1.setUnitTests(
            "import org.junit.jupiter.api.Test;\n" +
            "import static org.junit.jupiter.api.Assertions.*;\n\n" +
            "public class ArraySumTest {\n" +
            "    @Test\n" +
            "    void testBasicCase() {\n" +
            "        int[] array = {1, 2, 3, 4, 5, 6};\n" +
            "        assertEquals(12, ArraySum.sumEven(array));\n" +
            "    }\n" +
            "    @Test\n" +
            "    void testEdgeCase() {\n" +
            "        int[] array = {};\n" +
            "        assertEquals(0, ArraySum.sumEven(array));\n" +
            "    }\n" +
            "    @Test\n" +
            "    void testComplexCase() {\n" +
            "        int[] array = {2, 4, 6, 8, 10};\n" +
            "        assertEquals(30, ArraySum.sumEven(array));\n" +
            "    }\n" +
            "}"
        );
        exercise1.setSolution(
            "public class ArraySum {\n" +
            "    public static int sumEven(int[] array) {\n" +
            "        int sum = 0;\n" +
            "        for (int num : array) {\n" +
            "            if (num % 2 == 0) {\n" +
            "                sum += num;\n" +
            "            }\n" +
            "        }\n" +
            "        return sum;\n" +
            "    }\n" +
            "}"
        );
        exercise1.setConcepts("Java, Tableaux, Boucles, Conditions");
        exercise1.setExamples("Input: [1, 2, 3, 4, 5, 6] -> Output: 12");
        exercise1.setPublished(false); // Sauvegardé mais non publié
        exercise1.setCreator(teacher);
        exercise1.setCreatedAt(LocalDateTime.now().minusDays(2));
        exercise1.setUpdatedAt(LocalDateTime.now().minusDays(2));
        exerciseRepository.save(exercise1);

        // Exercise 2: Publié
        Exercise exercise2 = new Exercise();
        exercise2.setTitle("Inverser une chaîne de caractères");
        exercise2.setDescription("Implémenter une fonction qui inverse une chaîne de caractères");
        exercise2.setTopic("Strings");
        exercise2.setDifficulty(Exercise.DifficultyLevel.L1);
        exercise2.setProblemStatement(
            "Écrivez une fonction qui prend une chaîne de caractères en paramètre et retourne la chaîne inversée.\n\n" +
            "Exemple:\n" +
            "Input: \"hello\"\n" +
            "Output: \"olleh\""
        );
        exercise2.setStarterCode(
            "public class StringReverser {\n" +
            "    public static String reverse(String str) {\n" +
            "        // TODO: Implement your solution here\n" +
            "        return \"\";\n" +
            "    }\n" +
            "}"
        );
        exercise2.setUnitTests(
            "import org.junit.jupiter.api.Test;\n" +
            "import static org.junit.jupiter.api.Assertions.*;\n\n" +
            "public class StringReverserTest {\n" +
            "    @Test\n" +
            "    void testBasicCase() {\n" +
            "        assertEquals(\"olleh\", StringReverser.reverse(\"hello\"));\n" +
            "    }\n" +
            "    @Test\n" +
            "    void testEdgeCase() {\n" +
            "        assertEquals(\"\", StringReverser.reverse(\"\"));\n" +
            "    }\n" +
            "    @Test\n" +
            "    void testComplexCase() {\n" +
            "        assertEquals(\"dlrow olleh\", StringReverser.reverse(\"hello world\"));\n" +
            "    }\n" +
            "}"
        );
        exercise2.setSolution(
            "public class StringReverser {\n" +
            "    public static String reverse(String str) {\n" +
            "        if (str == null || str.isEmpty()) {\n" +
            "            return str;\n" +
            "        }\n" +
            "        StringBuilder reversed = new StringBuilder();\n" +
            "        for (int i = str.length() - 1; i >= 0; i--) {\n" +
            "            reversed.append(str.charAt(i));\n" +
            "        }\n" +
            "        return reversed.toString();\n" +
            "    }\n" +
            "}"
        );
        exercise2.setConcepts("Java, Strings, StringBuilder");
        exercise2.setExamples("Input: \"hello\" -> Output: \"olleh\"");
        exercise2.setPublished(true); // Publié
        exercise2.setCreator(teacher);
        exercise2.setCreatedAt(LocalDateTime.now().minusDays(1));
        exercise2.setUpdatedAt(LocalDateTime.now().minusDays(1));
        exerciseRepository.save(exercise2);

        // Exercise 3: Sauvegardé (not published) - Plus complexe
        Exercise exercise3 = new Exercise();
        exercise3.setTitle("Trouver le maximum dans un tableau");
        exercise3.setDescription("Implémenter une fonction qui trouve l'élément maximum dans un tableau");
        exercise3.setTopic("Tableaux");
        exercise3.setDifficulty(Exercise.DifficultyLevel.L2);
        exercise3.setProblemStatement(
            "Écrivez une fonction qui prend un tableau d'entiers en paramètre et retourne l'élément maximum.\n\n" +
            "Exemple:\n" +
            "Input: [3, 7, 2, 9, 1]\n" +
            "Output: 9"
        );
        exercise3.setStarterCode(
            "public class ArrayMax {\n" +
            "    public static int findMax(int[] array) {\n" +
            "        // TODO: Implement your solution here\n" +
            "        return 0;\n" +
            "    }\n" +
            "}"
        );
        exercise3.setUnitTests(
            "import org.junit.jupiter.api.Test;\n" +
            "import static org.junit.jupiter.api.Assertions.*;\n\n" +
            "public class ArrayMaxTest {\n" +
            "    @Test\n" +
            "    void testBasicCase() {\n" +
            "        int[] array = {3, 7, 2, 9, 1};\n" +
            "        assertEquals(9, ArrayMax.findMax(array));\n" +
            "    }\n" +
            "    @Test\n" +
            "    void testEdgeCase() {\n" +
            "        int[] array = {5};\n" +
            "        assertEquals(5, ArrayMax.findMax(array));\n" +
            "    }\n" +
            "    @Test\n" +
            "    void testComplexCase() {\n" +
            "        int[] array = {-10, -5, -1, -20};\n" +
            "        assertEquals(-1, ArrayMax.findMax(array));\n" +
            "    }\n" +
            "}"
        );
        exercise3.setSolution(
            "public class ArrayMax {\n" +
            "    public static int findMax(int[] array) {\n" +
            "        if (array == null || array.length == 0) {\n" +
            "            throw new IllegalArgumentException(\"Array cannot be empty\");\n" +
            "        }\n" +
            "        int max = array[0];\n" +
            "        for (int i = 1; i < array.length; i++) {\n" +
            "            if (array[i] > max) {\n" +
            "                max = array[i];\n" +
            "            }\n" +
            "        }\n" +
            "        return max;\n" +
            "    }\n" +
            "}"
        );
        exercise3.setConcepts("Java, Tableaux, Algorithmes");
        exercise3.setExamples("Input: [3, 7, 2, 9, 1] -> Output: 9");
        exercise3.setPublished(false); // Sauvegardé mais non publié
        exercise3.setCreator(teacher);
        exercise3.setCreatedAt(LocalDateTime.now().minusHours(5));
        exercise3.setUpdatedAt(LocalDateTime.now().minusHours(5));
        exerciseRepository.save(exercise3);

        logger.info("Created 3 test exercises: 2 saved (not published), 1 published");
    }
}

