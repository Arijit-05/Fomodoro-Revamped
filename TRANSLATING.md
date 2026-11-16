# Translating Fomodoro

Thank you for your interest in translating **Fomodoro**! This guide outlines how to contribute new translations or improve existing languages for Weblate, Crowdin, and Git pull requests.

---

## 1. Project Overview & Localization Architecture

Fomodoro uses modern Android localization with:
- **Zero hardcoded strings**: Every user-facing string is located in `res/values/strings.xml`.
- **Per-App Language Preferences**: Supports Android 13+ (API 33+) dynamic locale switching via `LocaleManager` and `res/xml/locales_config.xml`.
- **Right-To-Left (RTL) Support**: Layouts use start/end alignment, ensuring full compatibility with Arabic, Hebrew, and Persian.
- **Plurals & Interpolation**: Standard Android `<plurals>` and numbered string formatters (`%1$d`, `%2$s`).

---

## 2. Platform Integrations

### Weblate Integration
Weblate watches the `master` branch. The translation configuration is defined as:
- **Source file**: `app/src/main/res/values/strings.xml`
- **File mask**: `app/src/main/res/values-*/strings.xml`
- **Target format**: Android String Resource XML

### Crowdin Integration
Crowdin sync configuration (`crowdin.yml`):
```yaml
project_id: "fomodoro"
api_token_env: "CROWDIN_PERSONAL_TOKEN"
preserve_hierarchy: true

files:
  - source: /app/src/main/res/values/strings.xml
    translation: /app/src/main/res/values-%android_code%/strings.xml
```

---

## 3. Contributing via Git

1. Fork the repository and create a new branch:
   ```bash
   git checkout -b translation/add-<language-code>
   ```

2. Create a new directory under `app/src/main/res/`:
   ```bash
   # Example: Spanish
   mkdir -p app/src/main/res/values-es
   cp app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
   ```

3. Translate the string contents while preserving all format specifiers:
   ```xml
   <!-- Original (res/values/strings.xml) -->
   <string name="session_format">Session %1$d of %2$d</string>

   <!-- Spanish (res/values-es/strings.xml) -->
   <string name="session_format">Sesión %1$d de %2$d</string>
   ```

4. Add your locale code to `app/src/main/res/xml/locales_config.xml`:
   ```xml
   <locale android:name="es" />
   ```

5. Test the translation and submit a Pull Request!

---

## 4. Translation Guidelines

- **Placeholders**: Keep all `%1$s`, `%1$d`, etc., in the correct grammatical order for your language.
- **Apostrophes & Quotes**: Escape single quotes with a backslash (`\'`) or enclose string within double quotes.
- **Tone**: Keep labels concise, friendly, and distraction-free, matching the minimalist Pomodoro ethos.

