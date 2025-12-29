# 📝 Exemples de Sujets pour Génération d'Exercices

Ce document contient des exemples de descriptions que vous pouvez utiliser pour générer des exercices avec l'IA.

## 🎯 Comment utiliser

1. Connectez-vous en tant qu'enseignant
2. Allez sur "Créer un exercice"
3. Copiez-collez une des descriptions ci-dessous dans le champ de description
4. L'IA générera automatiquement :
   - L'énoncé complet
   - Le code de départ
   - Les tests unitaires
   - La solution
   - Les exemples

---

## 📚 Exemples par Niveau

### Niveau L1 (Débutant)

#### 1. Inverser une chaîne de caractères
```
Écrire une fonction qui inverse une chaîne de caractères. Par exemple, "hello" devient "olleh".
```

#### 2. Compter les mots dans une chaîne
```
Écrire une fonction qui compte le nombre de mots dans une chaîne de caractères. Les mots sont séparés par des espaces.
```

#### 3. Vérifier si un nombre est pair
```
Écrire une fonction qui vérifie si un nombre entier est pair. La fonction doit retourner true si le nombre est pair, false sinon.
```

#### 4. Calculer la somme d'un tableau
```
Écrire une fonction qui calcule la somme de tous les éléments d'un tableau d'entiers.
```

#### 5. Trouver le maximum dans un tableau
```
Écrire une fonction qui trouve et retourne l'élément maximum dans un tableau d'entiers.
```

#### 6. Vérifier si un tableau contient une valeur
```
Écrire une fonction qui vérifie si un tableau d'entiers contient une valeur donnée. Retourne true si la valeur est présente, false sinon.
```

#### 7. Compter les occurrences d'un caractère
```
Écrire une fonction qui compte combien de fois un caractère donné apparaît dans une chaîne de caractères.
```

#### 8. Calculer la moyenne d'un tableau
```
Écrire une fonction qui calcule la moyenne des éléments d'un tableau d'entiers.
```

---

### Niveau L2 (Intermédiaire)

#### 9. Vérifier si une chaîne est un palindrome
```
Écrire une fonction qui vérifie si une chaîne de caractères est un palindrome (se lit de la même manière dans les deux sens). Par exemple, "radar" est un palindrome.
```

#### 10. Trier un tableau d'entiers
```
Écrire une fonction qui trie un tableau d'entiers en ordre croissant. Utilisez l'algorithme de tri à bulles ou de tri par sélection.
```

#### 11. Rechercher un élément dans un tableau trié
```
Écrire une fonction qui recherche un élément dans un tableau d'entiers déjà trié. Utilisez la recherche binaire.
```

#### 12. Supprimer les doublons d'un tableau
```
Écrire une fonction qui supprime les doublons d'un tableau d'entiers et retourne un nouveau tableau sans doublons.
```

#### 13. Calculer le factoriel d'un nombre
```
Écrire une fonction récursive qui calcule le factoriel d'un nombre entier positif. Le factoriel de n (noté n!) est le produit de tous les entiers de 1 à n.
```

#### 14. Vérifier si deux chaînes sont des anagrammes
```
Écrire une fonction qui vérifie si deux chaînes de caractères sont des anagrammes (contiennent les mêmes lettres dans un ordre différent). Par exemple, "listen" et "silent" sont des anagrammes.
```

#### 15. Calculer la puissance d'un nombre
```
Écrire une fonction récursive qui calcule la puissance d'un nombre. La fonction prend deux paramètres : la base et l'exposant.
```

#### 16. Trouver les nombres premiers jusqu'à n
```
Écrire une fonction qui trouve et retourne tous les nombres premiers jusqu'à un nombre n donné.
```

---

### Niveau L3 (Avancé)

#### 17. Implémenter une pile (Stack)
```
Écrire une classe qui implémente une structure de données de type pile (stack) avec les opérations push, pop, peek et isEmpty.
```

#### 18. Implémenter une file (Queue)
```
Écrire une classe qui implémente une structure de données de type file (queue) avec les opérations enqueue, dequeue, front et isEmpty.
```

#### 19. Calculer le n-ième nombre de Fibonacci
```
Écrire une fonction qui calcule le n-ième nombre de la suite de Fibonacci. Utilisez la programmation dynamique pour optimiser.
```

#### 20. Vérifier si une chaîne contient des parenthèses équilibrées
```
Écrire une fonction qui vérifie si une chaîne de caractères contient des parenthèses équilibrées. Par exemple, "((()))" est équilibré, mais "((())" ne l'est pas.
```

#### 21. Trouver le plus grand commun diviseur (PGCD)
```
Écrire une fonction récursive qui calcule le plus grand commun diviseur (PGCD) de deux nombres entiers en utilisant l'algorithme d'Euclide.
```

#### 22. Trier un tableau de chaînes par longueur
```
Écrire une fonction qui trie un tableau de chaînes de caractères par ordre croissant de longueur. Si deux chaînes ont la même longueur, elles doivent être triées par ordre alphabétique.
```

#### 23. Compter les sous-chaînes dans une chaîne
```
Écrire une fonction qui compte combien de fois une sous-chaîne donnée apparaît dans une chaîne principale.
```

#### 24. Convertir un nombre en binaire
```
Écrire une fonction qui convertit un nombre entier décimal en sa représentation binaire (sous forme de chaîne de caractères).
```

---

## 💡 Conseils pour créer vos propres sujets

### Structure recommandée
1. **Action claire** : Commencez par "Écrire une fonction qui..." ou "Implémenter..."
2. **Description précise** : Décrivez ce que la fonction doit faire
3. **Exemple concret** : Ajoutez un exemple si possible

### Exemples de bonnes descriptions

✅ **Bien** :
```
Écrire une fonction qui prend un tableau d'entiers et retourne un nouveau tableau contenant uniquement les nombres pairs, dans le même ordre.
```

✅ **Bien** :
```
Implémenter une fonction qui vérifie si une chaîne de caractères contient uniquement des lettres (pas de chiffres ni de caractères spéciaux).
```

❌ **À éviter** :
```
Faire un truc avec des tableaux
```

❌ **À éviter** :
```
Exercice sur les boucles
```

---

## 🧪 Test de Concurrence

Pour tester la génération concurrente d'exercices, vous pouvez :

1. **Ouvrir deux onglets** dans votre navigateur
2. **Se connecter avec deux comptes enseignants différents** :
   - Compte 1 : `teacher` / `demo123`
   - Compte 2 : `teacher2` / `demo123`
3. **Générer des exercices simultanément** dans les deux onglets
4. **Observer les logs** pour voir que les requêtes sont traitées en parallèle

### Exemples pour test concurrent

**Onglet 1 (teacher)** :
```
Écrire une fonction qui calcule la somme des carrés des nombres pairs dans un tableau.
```

**Onglet 2 (teacher2)** :
```
Écrire une fonction qui trouve le plus petit élément dans un tableau d'entiers.
```

Les deux requêtes seront traitées en parallèle grâce à l'implémentation asynchrone avec `CompletableFuture` et `ExecutorService`.

---

## 📌 Notes

- Les descriptions peuvent être en français ou en anglais
- L'IA comprendra le contexte et générera du code Java approprié
- Plus la description est précise, meilleur sera le résultat
- Vous pouvez toujours modifier le code généré après la création

---

**Bon courage avec vos exercices ! 🚀**





