# 03 - Diagrammes UML

Les diagrammes sont écrits en Mermaid. GitHub les affiche directement. Chaque diagramme correspond à une question différente sur le système.

| # | Diagramme | Question à laquelle il répond |
| --- | --- | --- |
| 1 | Cas d'utilisation | que peut faire l'utilisateur ? |
| 2 | Classes | quelles sont les classes et leurs dépendances ? |
| 3 | Séquence : analyse réussie | que se passe-t-il, dans l'ordre, pour une image valide ? |
| 4 | Séquence : erreurs | comment chaque erreur remonte-t-elle jusqu'à la voix ? |
| 5 | États de l'interface | dans quel état se trouve la page ? |
| 6 | Activité : validation | quelles vérifications sont faites, et dans quel ordre ? |
| 7 | Composants et déploiement | quels processus communiquent, et sur quel port ? |
| 8 | Paquets | comment le code est-il découpé, et quelle règle de dépendance est respectée ? |
| 9 | Séquence : choix de l'agent | comment Spring choisit l'implémentation ? |

## 1. Cas d'utilisation

```mermaid
flowchart LR
    U(["Utilisateur"])
    subgraph Application["Décrire une image"]
        UC1(["Choisir une image"])
        UC2(["Poser une question facultative"])
        UC3(["Lancer l'analyse"])
        UC4(["Écouter la description"])
        UC5(["Relire la description"])
        UC6(["Arrêter la lecture"])
    end
    A(["Agent de description"])
    U --> UC1
    U --> UC2
    U --> UC3
    U --> UC4
    U --> UC5
    U --> UC6
    UC3 -.->|inclut| A
    UC4 -.->|suit| UC3
```

## 2. Classes

```mermaid
classDiagram
    direction LR

    class DescripteurImage {
        <<interface>>
        +decrire(image, question) Description
    }
    class OllamaDescripteur {
        -modele ChatModel
        +decrire(image, question) Description
    }
    class FauxDescripteur {
        +decrire(image, question) Description
    }
    class Image {
        <<record>>
        +octets byte[]
        +typeMime String
        +octets() byte[]
    }
    class Description {
        <<record>>
        +texte String
    }
    class DescriptionImpossibleException {
        <<exception vérifiée>>
    }
    class DescriptionControleur {
        -descripteur DescripteurImage
        +decrire(fichier, question) ResponseEntity
        +fichierTropGros() ResponseEntity
        +agentIndisponible() ResponseEntity
        +erreurInattendue() ResponseEntity
    }
    class ReponseDescription {
        <<record>>
        +description String
        +message String
    }
    class ChatModel {
        <<LangChain4J>>
    }

    DescripteurImage <|.. OllamaDescripteur
    DescripteurImage <|.. FauxDescripteur
    OllamaDescripteur --> ChatModel : adapte
    DescriptionControleur --> DescripteurImage : dépend de
    DescriptionControleur ..> ReponseDescription : renvoie
    DescripteurImage ..> Image : reçoit
    DescripteurImage ..> Description : renvoie
    DescripteurImage ..> DescriptionImpossibleException : lève

    note for OllamaDescripteur "Profil par défaut (!faux)"
    note for FauxDescripteur "Profil faux"
    note for DescriptionControleur "Ne connaît que l'interface"
```

## 3. Séquence : analyse réussie

```mermaid
sequenceDiagram
    actor U as Utilisateur
    participant P as Page web
    participant C as DescriptionControleur
    participant I as Image
    participant D as DescripteurImage

    U->>P: choisit un fichier, saisit une question, clique Analyser
    P->>P: affiche Analyse en cours et le lit
    P->>C: POST /api/descriptions (image, question)
    C->>I: new Image(octets, typeMime)
    I-->>C: image valide
    C->>C: question blanche devient null, sinon retirer les espaces
    C->>D: decrire(image, question)
    D-->>C: Description
    C-->>P: 200 {description, message: null}
    P->>P: afficherDescription
    P-->>U: texte affiché et lu à voix haute
```

## 4. Séquence : erreurs

```mermaid
sequenceDiagram
    participant P as Page web
    participant C as DescriptionControleur
    participant D as Agent

    P->>C: POST /api/descriptions
    alt fichier vide, type refusé ou signature incorrecte
        C-->>P: 400 {message}
    else question de plus de 500 caractères
        C-->>P: 400 {message}
    else agent indisponible
        C->>D: decrire(image, question)
        D-->>C: DescriptionImpossibleException
        C-->>P: 503 {message}
    else erreur inattendue
        C-->>P: 500 {message générique}
    else fichier de plus de 5 Mo
        C-->>P: 413 {message}
    end
    P->>P: afficherErreur, affiche le message et le lit
    P->>P: désactive le bouton Relire
```

## 5. États de l'interface

```mermaid
stateDiagram-v2
    [*] --> Attente
    Attente --> Analyse : Analyser avec un fichier
    Attente --> Attente : Analyser sans fichier, message lu
    Analyse --> Affichee : réponse 200
    Analyse --> Erreur : réponse 4xx ou 5xx, ou réseau coupé
    Affichee --> Affichee : Relire
    Affichee --> Analyse : Analyser à nouveau
    Erreur --> Analyse : Analyser à nouveau
    Affichee --> Attente : nouvelle page
    Erreur --> Attente : nouvelle page
```

## 6. Activité : validation d'une demande

```mermaid
flowchart TD
    A([Clic sur Analyser]) --> B{Fichier choisi ?}
    B -- non --> E1[Message lu : choisissez une image]
    B -- oui --> C[Construire le formulaire]
    C --> D{Question non vide ?}
    D -- oui --> D1[Ajouter la question sans espaces]
    D -- non --> D2[Pas de question]
    D1 --> F[POST /api/descriptions]
    D2 --> F
    F --> G{Taille 5 Mo maximum ?}
    G -- non --> E2[413 : image trop lourde]
    G -- oui --> H{Type annoncé pris en charge ?}
    H -- non --> E3[400 : type non pris en charge]
    H -- oui --> I{Signature conforme au type ?}
    I -- non --> E4[400 : contenu ne correspond pas]
    I -- oui --> J{Question 500 caractères maximum ?}
    J -- non --> E5[400 : question trop longue]
    J -- oui --> K[Agent : description]
    K --> L{Réponse produite ?}
    L -- oui --> M([Afficher et lire la description])
    L -- non --> E6[503 : agent indisponible]
    E1 --> Z([Message lu à voix haute])
    E2 --> Z
    E3 --> Z
    E4 --> Z
    E5 --> Z
    E6 --> Z
```

## 7. Composants et déploiement

```mermaid
flowchart LR
    subgraph Poste["Poste de l'utilisateur"]
        NAV["Navigateur<br/>index.html, app.js<br/>Web Speech API"]
    end

    subgraph Conteneur["Conteneur application (Docker)"]
        APP["Spring Boot<br/>port 8080"]
        AG["Agent DescripteurImage"]
    end

    subgraph Modele["Conteneur Ollama (Docker, GPU NVIDIA)"]
        OLL["Ollama<br/>port 11434<br/>modèle DESCRIPTEUR_OLLAMA_MODELE (llava par défaut)"]
    end

    NAV -- "POST multipart : image, question" --> APP
    APP -- "JSON : description, message" --> NAV
    APP --> AG
    AG -- "HTTP, DESCRIPTEUR_OLLAMA_HOTE:PORT" --> OLL
    OLL -- "texte" --> AG
```

## 8. Paquets

```mermaid
flowchart TB
    subgraph web["fr.imagedescription.web"]
        CTRL["DescriptionControleur"]
    end

    subgraph agent["fr.imagedescription.agent"]
        IF["DescripteurImage"]
        VO["Image, Description,<br/>DescriptionImpossibleException"]
        FAUX["FauxDescripteur"]
        OLL["OllamaDescripteur"]
    end

    subgraph lc4j["LangChain4J (bibliothèque externe)"]
        LIB["ChatModel, OllamaChatModel"]
    end

    CTRL --> IF
    CTRL --> VO
    FAUX -.->|implémente| IF
    OLL -.->|implémente| IF
    OLL --> LIB
    IF --> VO
```

**Règle vérifiée** : aucune flèche ne part de `web` vers `lc4j`, et seule `OllamaDescripteur` dépend de `lc4j`.

## 9. Choix de l'implémentation au démarrage

```mermaid
sequenceDiagram
    participant S as Spring
    participant CFG as application.properties
    participant FX as FauxDescripteur
    participant OL as OllamaDescripteur
    participant C as DescriptionControleur

    S->>CFG: lit les propriétés et les variables d'environnement
    alt profil faux actif
        S->>FX: crée le bean DescripteurImage
    else profil par défaut
        S->>OL: crée le bean avec url, modèle et délai
    end
    S->>C: injecte DescripteurImage par le constructeur
    C-->>S: contrôleur prêt
```
