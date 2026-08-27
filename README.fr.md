# Exemple AgentChatUI (AgentChatUI Sample)

Un **modèle d'interface de chat IA avec Streaming** pour Android, prêt à être cloné et entièrement construit avec Jetpack Compose.
Il est livré avec un backend simulé (mock) pour que vous puissiez tester l'expérience complète de chat immédiatement, puis connecter n'importe quel **Provider** de LLM (Gemini, OpenAI, un modèle local, ...) en implémentant une seule interface.

## Fonctionnalités

- **Réponses en Streaming** — les **Providers** émettent (**Emit**) des valeurs `Flow<ChatResponse>` (y compris `ChatResponse.Chunk`), qui sont rendues en direct avec des écritures de persistance régulées (250 ms).
- **Rendu Markdown** — gras, italique, code en ligne, titres, listes, citations, liens et aperçus d'images.
- **Blocs de code** — coloration syntaxique via highlight.js et rendu de **diagrammes Mermaid** dans une WebView, tous deux appuyés par un cache de rendu LRU.
- **Gestion des sessions** — panneau latéral avec les chats récents, nouveau chat, renommer, supprimer, et statistiques par session (nombre de messages, temps de réponse moyen, total de tokens, coût estimé).
- **Métadonnées des messages** — appuyez sur l'icône d'information d'un message pour inspecter le **Provider**, le modèle, les tokens (prompt/complétion/pensées), le temps de réponse et l'estimation du coût.
- **Pièces jointes** — jusqu'à 10 images/fichiers (20 Mo au total) depuis la galerie, les fichiers ou l'appareil photo.
- **Sélecteur de modèle** — trois niveaux de modèle plus un limiteur de longueur d'historique (20 / 50 / messages illimités envoyés comme contexte).
- **Arrêter la génération** — annulez un **Stream** en cours au milieu d'une réponse.
- **Commandes slash** — `/help`, `/clear`, `/code`, exemples de langages et guides hiérarchiques `/project/...` avec navigation cliquable.
- **Marqueurs d'étape de réflexion** — affiche des marqueurs de progression `THINKING_STEP` avant la réponse finale de l'IA.
- **Localisation** — anglais, coréen, chinois simplifié, japonais, espagnol, portugais brésilien, français et allemand.

Sans dépendances Room, Firebase, Hilt ou Navigation — juste Compose + coroutines + serialization.

## Commandes slash

Tapez `/` dans le champ de saisie pour voir les suggestions de commandes. Les suggestions sont filtrées par le texte saisi après le slash, et en sélectionner une place la commande complète dans le champ de saisie.

| Commande | Objectif |
|---|---|
| `/help` | Afficher toutes les commandes disponibles |
| `/clear` | Effacer l'historique du chat actuel |
| `/code` | Afficher des exemples de base pour chaque langage supporté |
| `/kotlin`, `/python`, `/java` | Afficher un exemple de base du langage |
| `/kotlin-long`, `/python-very-long`, etc. | Afficher des variantes de code plus longues |
| `/mermaid` | Afficher un exemple de diagramme Mermaid |
| `/project` | Ouvrir le guide d'intégration du projet |
| `/project/ai-connect/...` | Ouvrir un sujet spécifique de connexion IA |

Les réponses des commandes slash s'ouvrent dans une session d'explication séparée. Leur pied de page de métadonnées est masqué, et les chemins slash entre backticks dans la documentation sont cliquables.

## Structure du projet

```
com.b6star.chatui/
├─ App.kt                    # Entrée de l'application ; initialise ServiceLocator
├─ MainActivity.kt           # Héberge directement AgentScreen
├─ ai/                       # ★ Point d'extension — changez uniquement cette couche
│  ├─ AiGateway.kt           #    Contrat du **Provider** : chatStream() -> Flow<ChatResponse>
│  ├─ ChatResponse.kt        #    Chunk | Metadata | ShowDetails
│  ├─ Attachment.kt          #    AiImageAttachment / AiFileAttachment
│  ├─ AiModelCatalog.kt      #    ID des modèles affichés dans le sélecteur
│  └─ MockAiGateway.kt       #    Implémentation de démo (Streaming d'un contenu d'exemple)
├─ data/
│  ├─ model/ChatModels.kt    #    Classes de données ChatMessage / ChatSession
│  └─ ChatRepository.kt      #    Stockage en mémoire rempli avec une conversation d'exemple
├─ di/ServiceLocator.kt      # Injection manuelle de dépendances
├─ util/Utils.kt             # Aides pour l'estimation des coûts et le formatage
└─ ui/                       # Écran, ViewModel, rendu Markdown, WebView, dialogues, palette
```

## Connecter un **Provider** de LLM réel

1. Implémentez `AiGateway` :

```kotlin
class GeminiGateway : AiGateway {
    override fun chatStream(
        history: List<ChatMessage>,
        model: String,
        historyLimit: Int,
        images: List<AiImageAttachment>,
        files: List<AiFileAttachment>
    ): Flow<ChatResponse> = flow {
        // Appelez l'API de Streaming de votre SDK ici et traduisez chaque delta en :
        emit(ChatResponse.Chunk(textDelta))
        // Optionnel, une fois à la fin (**Emit**) :
        emit(ChatResponse.Metadata(promptTokens = ..., candidatesTokens = ...,
                                   responseTimeMs = ..., modelName = model, provider = "gemini"))
    }
}
```

2. Modifiez une ligne dans `di/ServiceLocator.kt` :

```kotlin
val aiGateway: AiGateway by lazy { GeminiGateway() }   // était MockAiGateway()
```

C'est tout — l'UI, le pipeline de Streaming, l'affichage des métadonnées et la gestion des sessions continuent de fonctionner sans changement.

### Contrat de Streaming et de réponse

`AiGateway.chatStream()` renvoie `Flow<ChatResponse>`. Un **Provider** doit convertir chaque delta du SDK ou **Chunk** de réponse en `ChatResponse.Chunk(text)`, émettre (**Emit**) des métadonnées lorsque les informations d'utilisation sont disponibles, et relancer `CancellationException` pour que l'arrêt de la génération fonctionne correctement.

Le `MockAiGateway` fourni illustre le même contrat avec des **Chunks** déterministes. Il s'agit intentionnellement d'un mock en mémoire qui n'appelle pas de service d'IA externe.

### Notes sur le contrat

- Émettez (**Emit**) `Chunk` de manière répétée pendant la génération ; le ViewModel les accumule et les enregistre dans le stockage toutes les 250 ms.
- L'émission (**Emit**) de `Metadata` avant la fin du **Stream** remplit le pied de page de la bulle (tokens, coût, latence). Omettez-le si votre **Provider** ne fournit pas de statistiques d'utilisation.
- Émettez (**Emit**) des éléments `ShowDetails` pour activer le dialogue en ligne "[Voir les détails]".
- L'annulation (`stopGeneration()`) annule simplement la coroutine de collecte — nettoyez votre **Stream** amont (upstream flow) en conséquence.
- Le dépôt actuel est en mémoire (les données sont réinitialisées à la fermeture du processus). Pour persister les données, remplacez l'intérieur de `ChatRepository` par Room ou DataStore tout en conservant son API publique.

## Notes sur Markdown et Mermaid

- Les exemples de code dans les ressources string Android utilisent `\u0020` pour les espaces de début, car les espaces littéraux répétés peuvent être supprimés lors du traitement des ressources et du HTML.
- Les étiquettes de nœuds Mermaid contenant des guillemets utilisent `\u0022`, par exemple `A[\u0022Analyser les blocs\u0022]`.
- L'analyseur Markdown personnalisé prend en charge le code en ligne avec un simple backtick, comme `` `Code en ligne` ``. Évitez d'envelopper cette forme dans une paire supplémentaire de backticks.
- `AgentMarkdown.kt` contrôle l'analyse Markdown, les symboles de titre/liste, la détection de `THINKING_STEP`, les chemins slash cliquables et le formatage en ligne. `AgentWebView.kt` contrôle la coloration syntaxique, le rendu Mermaid et le cache de rendu.

## Démarrage

**Configuration requise :** Android Studio (dernière version), JDK 17+, Android SDK 37

```bash
git clone <votre fork>
# ouvrez le dossier dans Android Studio et lancez ▶
```

ou depuis la ligne de commande :

```bash
./gradlew assembleDebug
```

| Config | Valeur |
|---|---|
| Paquet | `com.b6star.chatui` |
| minSdk / targetSdk | 26 / 37 |
| Kotlin / AGP | 2.2.10 / 9.3.2 |
| Compose BOM | 2026.02.01 |

## Remarques

- highlight.js et Mermaid sont chargés depuis un CDN au moment de l'exécution, donc le rendu des blocs de code nécessite un accès réseau au premier rendu (les résultats sont mis en cache par la suite).
- Les coûts de tokens sont des estimations approximatives calculées dans `util/Utils.kt` avec des prix fictifs — ajustez selon votre **Provider**.
