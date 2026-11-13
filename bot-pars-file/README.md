# ⚙️ BOT-PARS-FILE

Проект представляет собой телеграм‑бота на TypeScript, интегрирующего Python‑скрипты для получения и формирования актуального расписания с сайта колледжа, а также Java‑модуль для дальнейшего парсинга данных.
Архитектура решения построена на разделении ответственности:
- TypeScript — логика бота и взаимодействие с пользователем - отправка готового рассписания.
- Python — загрузка и подготовка файлов с сайта, конечное структурирование данных, которые спарсены Java скриптом.
- Java — парсинг файлов Doc/Docx

## 💡 Установка и запуск

1. Клонируйте репозиторий:
```
git clone https://github.com/FeodorCore/Bot-pars-file
```

2. Установите Python-зависимости:
```
pip install -r requirements.txt
```

3. Скомпилируйте Java-парсер (Maven автоматически подтянет все зависимости):
```
mvn clean package
```
3. Установите Node.js-зависимости:
```
npm install
```
4. Скомпилируйте TypeScript в JavaScript:
```
npm run build 
```
5. В файл .env добавьте сгенерированный токен из BotFather:
```
BOT_TOKEN=ЗДЕСЬ ВСТАВЬТЕ ТОКЕН
```
6. Запустите бота:
```
node dist/bot.js
```

## 🎮 Команды

- `/start`: Запуск бота.
- `/get`: Получить рассписание.

## 📲 Диалог с ботом
<img src="assets/screenshot.png" alt="Скриншот диалога с ботом" width="300"/>

## 🔄 Принцип работы
<img src="assets/image.png" alt="Скриншот принципа работы" width="500"/>

## 📁 Структура проекта
```
|    |—— Bot-pars-file.iml
|—— requirements.txt
|—— running_scripts.py
|—— Scripts
|    |—— Input-Output
|        |—— Download-doc
|        |—— json-file
|        |—— yaml-file
|    |—— Java-parser
|        |—— .gitignore
|        |—— dependency-reduced-pom.xml
|        |—— pom.xml
|        |—— src
|            |—— main
|                |—— java
|                    |—— org
|                        |—— example
|                            |—— common
|                                |—— DataWriter.java
|                                |—— DocumentContent.java
|                                |—— DocumentParseException.java
|                                |—— DocumentProcessor.java
|                                |—— FileManager.java
|                                |—— FileNameReader.java
|                                |—— JsonManager.java
|                                |—— LessonData.java
|                                |—— Parser.java
|                                |—— ScheduleLineProcessor.java
|                                |—— TextProcessor.java
|                                |—— YamlManager.java
|                            |—— Doc
|                                |—— DocumentParser.java
|                            |—— Docx
|                                |—— DocxDocumentParser.java
|                            |—— Main.java
|    |—— Python-downloader
|        |—— main.py
|    |—— Python-formatted
|        |—— main.py
|    |—— TypeScript-tgBot
|        |—— src
|            |—— bot.ts
|            |—— commands.ts
|            |—— handlers
|                |—— index.ts
|                |—— schedule.ts
|                |—— start.ts
|            |—— keyboard.ts
|            |—— services
|                |—— pythonRunner.ts
|            |—— utils
|                |—— helpers.ts
|        |—— tsconfig.json
```
