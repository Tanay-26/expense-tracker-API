# Complete Beginner Setup Guide — Mac

This guide assumes you have **never** run a Java project, never used Docker, and never
opened Terminal for anything beyond basic file browsing. Every step is spelled out.

---

## PHASE 1 — Install the four tools you need (one-time, ~20 minutes)

You need exactly four things installed. Nothing else.

| Tool | What it is in plain English | Why you need it |
|---|---|---|
| **Homebrew** | A package manager for Mac. It installs developer tools with one command instead of hunting for download links. | Makes installing everything else trivial. |
| **JDK 17** | The Java engine. Your code is written in Java; the JDK is what turns it into something your Mac can run. | Without it, Maven has nothing to compile with. |
| **Maven** | A build tool. Think of it as an assistant that downloads all the libraries your project needs, compiles your code, runs your tests, and starts the app — all with one command. | The project's `pom.xml` is Maven's instruction sheet. |
| **Docker Desktop** | Runs mini-computers (called "containers") inside your Mac. Instead of installing PostgreSQL and Kafka natively (painful), you run them inside Docker — isolated, disposable, identical every time. | `docker-compose.yml` tells Docker exactly what to spin up. |

### 1A. Install Homebrew (if you don't have it)

1. Open **Terminal** — press **Cmd + Space**, type `Terminal`, press Enter.

2. Paste this entire line and press Enter:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

3. It will ask for your Mac password (the one you use to log in). **You won't see any characters as you type** — that's normal. Type it and press Enter.

4. It may take a few minutes. When it finishes, **read the output carefully**. On Apple Silicon Macs (M1/M2/M3/M4), it prints two commands under "Next steps" that you must run. They look like:

```bash
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"
```

Copy and run both of those lines exactly as shown. Without this, your Terminal won't find `brew`.

5. Verify:

```bash
brew --version
```

You should see a version number like `Homebrew 4.x.x`.

### 1B. Install JDK 17 and Maven

One command installs both:

```bash
brew install openjdk@17 maven
```

Then link Java so your system can find it:

```bash
sudo ln -sfn $(brew --prefix openjdk@17)/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
```

It asks for your password again. Type it and press Enter.

Now set JAVA_HOME permanently. Run this single line:

```bash
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc && source ~/.zshrc
```

Verify both:

```bash
java -version
mvn -version
```

`java -version` should say `openjdk version "17.x.x"`.
`mvn -version` should say `Apache Maven 3.9.x` and mention Java 17.

**If `java -version` still shows an old version** (like Java 8 or 11), close Terminal completely (Cmd + Q), reopen it, and try again.

### 1C. Install Docker Desktop

1. Go to **https://www.docker.com/products/docker-desktop/**

2. Click the download button. It will auto-detect whether you have an **Apple Silicon** (M1/M2/M3/M4) or **Intel** Mac and give you the right file.

   Not sure which chip you have? Click the Apple logo (top-left of screen) → **About This Mac**. If it says "Apple M1" / "Apple M2" etc., you have Apple Silicon. If it says "Intel", you have Intel.

3. Open the downloaded `.dmg` file. Drag the Docker icon into the Applications folder.

4. Open Docker from **Applications** (or Spotlight: Cmd + Space → type "Docker" → Enter).

5. It may ask for permission and your password. Grant everything.

6. **Wait** until the Docker icon in your menu bar (top-right of screen) stops animating and the tooltip says **"Docker Desktop is running"**. This takes 30–60 seconds.

7. Verify in Terminal:

```bash
docker --version
docker compose version
```

Both should print version numbers.

**Important:** Docker Desktop must be **running** every time you work on this project. If you restart your Mac, open Docker Desktop again before doing anything else.

### 1D. VS Code extensions (optional but helpful)

Open VS Code → click the Extensions icon (left sidebar, looks like 4 squares) → search and install:

- **Extension Pack for Java** (by Microsoft) — syntax highlighting, error squiggles, run buttons
- **REST Client** (by Huachao Mao) — lets you click "Send Request" inside `requests.http` to test endpoints
- **Docker** (by Microsoft) — shows running containers in the sidebar

---

## PHASE 2 — Open the project in VS Code (~2 minutes)

1. You downloaded `expense-tracker-api.zip`. Double-click it to extract (Mac does this automatically into the same folder).

2. Move the extracted `expense-tracker-api` folder somewhere you'll remember. Suggestion:

```bash
mkdir -p ~/Projects
mv ~/Downloads/expense-tracker-api ~/Projects/
```

3. Open VS Code.

4. **File → Open Folder** → navigate to `~/Projects/expense-tracker-api` (the folder that directly contains `pom.xml`, `docker-compose.yml`, etc.) → click **Open**.

5. VS Code may pop up: "Do you trust the authors?" → click **Yes**.

6. If the Java extension asks to import the project or says "A build file was found" → click **Yes** / **Import**. This tells VS Code to read `pom.xml` and understand the project structure.

7. You should now see all the files in the left sidebar.

---

## PHASE 3 — Start the infrastructure (PostgreSQL + Kafka) (~3 minutes)

Your API needs a database (PostgreSQL) and a message broker (Kafka) running. Docker starts both in containers.

1. Open the terminal inside VS Code: press **Ctrl + `** (backtick — the key below Esc, next to the number 1).

2. Make sure you're in the project folder. The prompt should show something like:

```
~/Projects/expense-tracker-api $
```

If not, type:

```bash
cd ~/Projects/expense-tracker-api
```

3. Run:

```bash
docker compose up -d
```

**What this does, in plain English:**
- Reads `docker-compose.yml`
- Downloads the PostgreSQL and Kafka images (first time only — ~500 MB, takes a few minutes on slow Wi-Fi)
- Starts three containers: `expense-postgres`, `expense-kafka`, `expense-kafka-ui`
- The `-d` means "detached" — they run in the background so you get your terminal back

4. Wait for it to finish. You'll see lines like `✔ Container expense-postgres Started`. Then verify:

```bash
docker compose ps
```

You should see three containers, all with status **"running"**:

```
NAME                STATUS
expense-kafka       running
expense-kafka-ui    running
expense-postgres    running (healthy)
```

If postgres says **"starting"** instead of **"healthy"**, wait 10 seconds and run `docker compose ps` again.

5. Quick sanity check — is the database alive?

```bash
docker compose exec postgres psql -U expense_user -d expense_tracker -c "SELECT 1;"
```

If you see a little table with `1` in it, Postgres is working.

---

## PHASE 4 — Build and test the project (~2 minutes)

Now compile all the Java code and run the automated tests.

1. In the same terminal, run:

```bash
mvn clean verify
```

**What this does:**
- `clean` — deletes any old compiled files (the `target/` folder)
- Downloads every library listed in `pom.xml` (first time: ~200 MB of `.jar` files)
- Compiles all `.java` files
- Runs all 14 unit tests

**First time will be slow** (1–3 minutes) because Maven downloads everything. After that, it takes ~15 seconds.

2. At the end you should see:

```
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**If you see BUILD FAILURE**, check the troubleshooting section at the bottom.

---

## PHASE 5 — Start the API server (~1 minute)

```bash
mvn spring-boot:run
```

The terminal fills with log lines. Look for these key lines:

```
Successfully applied 1 migration to schema "public"     ← Flyway created the tables
Tomcat started on port 8080                              ← API is live
partitions assigned: [expense-events-0, ...]             ← Kafka consumers are listening
Started ExpenseTrackerApplication in X.XX seconds        ← done!
```

**The terminal is now occupied** — the server is running inside it. You can't type other commands here. That's normal.

To run commands, open a **second terminal tab**: click the **+** icon at the top-right of the terminal panel, or press **Ctrl + Shift + `**.

To stop the server later: click back into the first terminal and press **Ctrl + C**.

---

## PHASE 6 — Test every endpoint (~5 minutes)

Open a **second terminal tab** (the first one is running your server).

### Step 1 — Register a user and save the token

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"riya@example.com","password":"sup3rsecret","fullName":"Riya Sharma"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')

echo $TOKEN
```

The `echo` should print a long string starting with `eyJ...`. If it prints nothing or an error, the server isn't running — go back to Phase 5.

**If you've already registered** (you'll get a 409 error), use login instead:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"riya@example.com","password":"sup3rsecret"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
```

### Step 2 — Verify security works (should fail)

```bash
curl -s http://localhost:8080/api/v1/expenses
```

You should see `"status":401,"error":"UNAUTHORIZED"`. Good — endpoints are protected.

### Step 3 — Set a monthly FOOD budget of ₹5,000

```bash
curl -s -X PUT http://localhost:8080/api/v1/budgets \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"category":"FOOD","monthlyLimit":5000.00}'
```

Should return the budget object with `"monthlyLimit":5000.00`.

### Step 4 — Create a small expense (under budget, no alert)

```bash
curl -s -X POST http://localhost:8080/api/v1/expenses \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":450.00,"category":"FOOD","description":"Groceries","spentAt":"2026-08-20"}'
```

Look at your **server terminal** (the first tab). You'll see:

```
Published eventId=... to expense-events-1 @offset 0
Evaluating budget for eventId=... userId=1 category=FOOD
userId=1 at 9% of FOOD budget - below threshold
```

No alert — you're only at 9%.

### Step 5 — Create a BIG expense that blows past the budget

```bash
curl -s -X POST http://localhost:8080/api/v1/expenses \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":5000.00,"category":"FOOD","description":"Party dinner","spentAt":"2026-08-21"}'
```

**Now watch your server terminal.** You should see the entire pipeline fire:

```
Published eventId=abc123 to expense-events-1 @offset 1
Evaluating budget for eventId=abc123 userId=1 category=FOOD
Budget alert raised: userId=1 category=FOOD usage=109%
Published eventId=def456 to notification-events-2 @offset 0
NOTIFICATION DISPATCHED -> to=riya@example.com | You have exceeded your Food budget for 2026-08. Spent 5,450.00 of your 5,000.00 limit (109%).
```

**That's the Kafka pipeline working in real time.** The user's HTTP request returned in under 100ms. The budget check and notification happened asynchronously on other threads.

### Step 6 — See the notification the pipeline created

```bash
curl -s http://localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

(The `| python3 -m json.tool` part just makes the JSON output pretty-printed and readable.)

You should see the budget alert notification sitting there.

### Step 7 — Check the unread badge count

```bash
curl -s http://localhost:8080/api/v1/notifications/unread-count \
  -H "Authorization: Bearer $TOKEN"
```

Returns something like `{"unread":1}`.

### Step 8 — List expenses (filtered + paginated)

```bash
curl -s "http://localhost:8080/api/v1/expenses?category=FOOD&from=2026-08-01&to=2026-08-31" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### Step 9 — Monthly summary (the GROUP BY aggregation)

```bash
curl -s "http://localhost:8080/api/v1/expenses/summary?month=2026-08" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

Shows total spend and a breakdown by category.

### Step 10 — Test validation (should fail with 400)

```bash
curl -s -X POST http://localhost:8080/api/v1/expenses \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":-5,"category":"FOOD","spentAt":"2099-01-01"}' | python3 -m json.tool
```

Returns `400` with a `details` array explaining: amount must be greater than 0, spentAt cannot be in the future.

### Step 11 — See Kafka messages visually

Open your browser and go to **http://localhost:8081**

This is Kafka UI. Click **Topics** in the left sidebar → click **expense-events** → click **Messages**. You'll see the actual JSON events your API published. Do the same for **notification-events** to see the budget alerts.

### Step 12 — Check API latency metrics

```bash
curl -s http://localhost:8080/actuator/metrics/http.server.requests | python3 -m json.tool
```

This shows real p95/p99 latency numbers from Spring Actuator.

---

## PHASE 7 — Stopping everything when you're done

**Stop the API server:**
Click into the terminal where it's running → press **Ctrl + C**.

**Stop Docker containers:**

```bash
docker compose down
```

Stops and removes containers but **keeps your data** in Docker volumes.

**To also delete all data** (fresh start next time):

```bash
docker compose down -v
```

---

## Starting again next session

Every time you sit down to work on this project:

```bash
# 1. Open Docker Desktop (from Applications or Spotlight) and wait for it to be "running"

# 2. Open VS Code → open the project folder → open terminal (Ctrl + `)

# 3. Start infrastructure
docker compose up -d

# 4. Start the API
mvn spring-boot:run

# 5. In a second terminal tab, get a token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"riya@example.com","password":"sup3rsecret"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')

# 6. Make requests using $TOKEN
```

If you wiped data with `docker compose down -v`, use `/auth/register` instead of `/auth/login`.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `brew: command not found` | Homebrew installed but shell not configured | Run the two `echo` and `eval` lines from step 1A again, then open a new Terminal |
| `java: command not found` | JDK not linked | Run the `sudo ln -sfn ...` command from step 1B again |
| `java -version` shows Java 8 or 11 | Old Java installation taking priority | Run `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` and add it to `~/.zshrc` |
| `mvn: command not found` | Maven not installed | Run `brew install maven`, open new terminal |
| `docker: command not found` | Docker Desktop not installed or not running | Install it, open it, wait for "running" status |
| `Cannot connect to the Docker daemon` | Docker Desktop is not running | Open Docker Desktop from Applications, wait for green/running status |
| `port 5432 already in use` | Another Postgres is running on your Mac | Run `lsof -i :5432` to find it, then `kill <PID>`, or change the port in `docker-compose.yml` to `"5433:5432"` and update `DB_URL` in `application.yml` to use port `5433` |
| `port 9092 already in use` | Another Kafka is running | Run `lsof -i :9092` to find it, then `kill <PID>` |
| `BUILD FAILURE` + connection refused | Docker containers aren't running | Run `docker compose up -d` and wait for "healthy" |
| `401 Unauthorized` on every request | Token missing, expired, or wrong | Re-run the `TOKEN=$(curl ...)` command from Step 1 |
| Server starts but no Kafka log lines | Kafka container crashed | Run `docker compose logs kafka` to see the error |
| `WeakKeyException` on startup | JWT secret is too short | Use the default one in `application.yml` or generate a new one: `openssl rand -base64 48` |
| `409 CONFLICT` on register | Account already exists | Use `/auth/login` instead, or wipe data with `docker compose down -v` then `docker compose up -d` |
| `mvn spring-boot:run` hangs at "Downloading" | Slow internet, Maven is fetching dependencies | Wait — first run downloads ~200 MB. Subsequent runs are instant. |
| Everything worked yesterday, nothing works today | Docker Desktop isn't running | Open Docker Desktop, wait for green icon, then `docker compose up -d` |
