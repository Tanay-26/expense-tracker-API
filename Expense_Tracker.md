# Expense Tracker API — Complete Interview Preparation Guide

**Project:** Expense Tracker API — Event Driven Backend (Java, Kafka)
**Timeline on resume:** Mar 2026 – May 2026

> **How to use this document.** Read Parts 1–5 once, slowly, to actually understand the project.
> Then use Part 6 (the 50 questions) for repeated revision. Don't memorise word-for-word — read
> an answer, close the file, and say it out loud in your own words. If you can do that, you're ready.

---

# TABLE OF CONTENTS

| Part | What it covers |
|---|---|
| **Part 1** | What this project is — explained like you're 5 |
| **Part 2** | Why every technology was chosen (with comparisons) |
| **Part 3** | Every tech term explained in simple English (glossary) |
| **Part 4** | The complete flow — what happens end to end |
| **Part 5** | Every file explained — what it does and why it exists |
| **Part 6** | 50 interview questions with human-sounding answers |
| **Part 7** | Your 2-minute project pitch (memorise this) |
| **Part 8** | Final checklist before the interview |

---

# PART 1 — WHAT THIS PROJECT IS (Explained Simply)

## 1.1 First — what is a "backend"?

Every app you use has two halves.

**The frontend** is what you see. The buttons, the colours, the screens. When you open Google Pay
and see your balance on screen — that screen is frontend.

**The backend** is everything you *don't* see. It stores your data, checks your password, does the
maths, and decides what you're allowed to do. It has no appearance at all. It's just code sitting
on a server.

**You built the backend.** There is no app to look at. There are no screens. If your interviewer
asks "can I see it?", the answer is: "It's an API — I can show you the requests and responses, or
show you the server logs while I hit it."

Say this clearly and confidently. A lot of students get flustered here. There's nothing wrong with
having built only a backend. Backend is where the actual engineering is.

## 1.2 What is an "API"?

API stands for Application Programming Interface. Ignore the long name.

**An API is a menu of things one program will do for another program.**

Just like a restaurant menu tells you what you can order, an API tells other programs what they can
ask for. Your API's menu looks like this:

| What someone can ask for | The "menu item" (endpoint) |
|---|---|
| Make me an account | `POST /api/v1/auth/register` |
| Let me log in | `POST /api/v1/auth/login` |
| Save this ₹450 grocery expense | `POST /api/v1/expenses` |
| Show me my expenses | `GET /api/v1/expenses` |
| Show my August total by category | `GET /api/v1/expenses/summary?month=2026-08` |
| Set a ₹5000 monthly food limit | `PUT /api/v1/budgets` |
| Do I have any alerts? | `GET /api/v1/notifications` |

A human never types these. A **frontend app** (a website or mobile app) sends these requests over
the internet. Your backend does the work and sends back an answer.

## 1.3 What does the answer look like?

Your backend replies in **JSON**. JSON is just text arranged in a way programs can read easily.

```json
{
  "id": 12,
  "amount": 450.00,
  "category": "FOOD",
  "description": "Groceries",
  "spentAt": "2026-08-20"
}
```

That's it. It's like a labelled form. `"amount": 450.00` means "the field called amount has the
value 450.00". Nothing more complicated than that.

## 1.4 So what does YOUR project actually do?

In plain words:

1. **People can create an account and log in.** Passwords are stored scrambled, never as plain text.
2. **They can record their spending.** "I spent ₹450 on food on 20th August."
3. **They can set monthly limits.** "Don't let me spend more than ₹5000 on food per month."
4. **The system watches their spending in the background.** When they cross 80% of a limit, it
   warns them. When they cross 100%, it tells them they've gone over.
5. **They can see summaries.** "In August I spent ₹12,000 total — ₹5,450 on food, ₹4,000 on rent..."

**The clever part — and this is the whole point of the project:** step 4 does *not* happen while the
user waits. The user saves their expense and gets an instant reply. The budget checking and the
alert happen a second later, in the background, on a completely different part of the system.

That "background pipeline" is what Kafka does. It's the heart of the project and it's what makes
this an **event-driven** system rather than a basic CRUD app.

## 1.5 Why is that background part such a big deal?

Let me show you the problem it solves with a story.

**Month 1.** You build `POST /expenses`. It saves one row to the database. Takes **10 milliseconds**.
(A millisecond is 1/1000th of a second. 10ms is instant.)

**Month 2.** Your boss says "add budget alerts". The obvious thing to do is add the code right there:
save the expense → calculate this month's total → compare with the budget → send an email.

Now your endpoint takes:
- 10ms to save
- 40ms to calculate the total
- **400ms waiting for the email company's server to respond**
- = **450 milliseconds**

The user is now staring at a loading spinner for half a second, because your system decided to send
them an email *while they wait*.

**Month 3.** "Add analytics." Another 150ms. Now **600ms**.

**Month 4.** "Feed it to the data science team." **750ms.**

Three things have gone wrong, and they're all serious:

**Problem 1 — It gets slower forever.** Every new feature adds time to the user's wait. There's no
limit. In two years this endpoint takes 3 seconds.

**Problem 2 — Small failures become big failures.** If the email company's server is down, your
`POST /expenses` crashes. **The user cannot record that they bought groceries — because an email
service is broken.** A tiny, unimportant feature has taken down your most important one. This
sentence is gold in an interview. Say it exactly like that.

**Problem 3 — Changing anything is scary.** Every new feature means editing, retesting and
redeploying the most important piece of code in your system. The code that matters most becomes
the code you touch most often. That's backwards.

**Event-driven architecture fixes all three with one idea:**

> The API's only job is to **save the fact and announce it happened**. Then it's done.

The announcement is called an **event**. It gets dropped onto Kafka, which stores it safely on disk.
Separate programs (**consumers**) pick it up whenever they're ready and do the slow work on their
own time.

What you get:

- `POST /expenses` stays **under 100ms forever**, no matter how many features you add later.
- Email service down for 20 minutes? The events wait patiently in Kafka. When it comes back, it
  catches up. **Nothing is lost and no user noticed anything.**
- Adding a new feature = writing a new consumer. **You never touch the expense-saving code again.**

That last point is the real definition of "decoupled". It's the difference between coding and
engineering.

## 1.6 The honest downside (say this in interviews)

Event-driven isn't free. Be ready to say this, because it proves you understand the trade-off and
didn't just copy a design pattern:

- You now have to run and maintain a Kafka cluster. That's real operational work.
- Your system is **eventually consistent**. For a second or two after saving, the expense exists but
  the alert doesn't. That's fine for alerts. It would NOT be fine for something like "did my payment
  go through".
- Debugging is harder. Instead of reading one function top to bottom, you're tracing a flow across
  three separate programs.

**For a system with one side effect and ten users, this is over-engineering.** It becomes the right
choice when side effects multiply. Saying this out loud makes you sound senior.

---

# PART 2 — THE RESTAURANT ANALOGY (The Full Picture)

This is the single best way to explain your project. Memorise this — it works on technical and
non-technical interviewers alike.

Imagine a busy restaurant.

## The customer walking in = **The Frontend App**

They don't cook. They don't know where the kitchen is. They never touch the record book. They just
ask for things.

In your project this is a React website or a mobile app. **You did not build this.** You built
everything the customer interacts with once they're inside.

## The bouncer at the door = **JWT (your security)**

Nobody gets past the door without showing a VIP wristband.

Here's the clever bit about this particular wristband. When you first showed your ID at the entrance
(that's logging in), you were given a wristband with a **tamper-proof holographic stamp**. Now, every
time you walk up to the counter, the bouncer glances at the stamp and instantly knows it's genuine.

**He does not phone the manager to check the guest list.** The stamp itself is the proof.

That's exactly what a JWT is. It's a token with a mathematical signature. Your server can verify it
instantly without looking anything up in the database. This is why your security code does **zero
database queries** — the wristband already says "this is customer #42" and the signature proves
nobody forged it.

Two things follow from this:
- **It's fast.** No database lookup on every single request.
- **You can't rip the wristband off remotely.** If someone steals it, it works until it expires.
  That's why yours expires after 60 minutes.

## The waiter = **Your Java / Spring Boot API. This is what you built.**

The waiter is the only person the customer talks to. He:
- Takes the order
- Checks it makes sense ("you can't order negative curry" — that's your validation)
- Writes it in the record book
- Comes back with a receipt

**Critically: the waiter never cooks.** His job is receiving, checking, recording, and replying.

In your code, this is `ExpenseController` (takes the order) and `ExpenseService` (does the work).

## The indestructible ledger book = **PostgreSQL (your database)**

A heavy, permanent book in the back where every order is written in ink.

Three things make it special:

**It survives everything.** Restaurant burns down? The book is fine. Server reboots? Your data is
still there. This is what "persistent storage" means.

**It has rules printed into the paper itself.** Certain lines physically cannot be written:
- "No order for a negative amount" → your `CHECK (amount > 0)` rule
- "No two budgets for the same customer and same category" → your `UNIQUE` rule

This matters because the rules live **in the book**. Even if you hire a new waiter tomorrow who
forgets the rules, the book itself refuses the bad entry. That's much safer than hoping every
programmer remembers.

**Entries are all-or-nothing.** You never end up with half an order written down. If something goes
wrong mid-write, the whole thing is erased and it's like it never happened. That's called a
**transaction** — in your code it's the `@Transactional` label.

## The conveyor belt = **Kafka. This is the heart of your project.**

Here's the contrast that makes everything click.

**A bad restaurant:** the waiter takes your order, writes it in the book, then *personally walks to
the kitchen, cooks the food, plates it, walks to the mailroom, writes you a thank-you note, posts
it* — and only THEN comes back and hands you your receipt. You stand at the counter for ten minutes
waiting for a piece of paper.

That's the **synchronous** version. That's the 450ms endpoint.

**Your restaurant:** the waiter writes it in the book, scribbles a ticket, **slaps it onto a
fast-moving conveyor belt**, and immediately says "Next!". You have your receipt in a fraction of a
second.

**The waiter never waits for the kitchen.**

Four things about this specific belt matter. Each one maps to something real in Kafka:

**1. Tickets never fall off.** The belt is bolted down and fireproof. Even if every single chef goes
on a tea break, the tickets sit there safely waiting.
→ *Kafka writes every message to disk. Nothing is lost if a program crashes.*

**2. Tickets are NOT removed when someone reads them.** A chef reads a ticket and leaves it there.
So the grill chef, the dessert chef, and the accountant can each read the same ticket independently.
Each one keeps their own bookmark of how far they've got.
→ *This is the single biggest difference between Kafka and a normal queue, and it's why you can add
new features later without touching old code.*

**3. One customer's tickets always stay in order.** Tickets are sorted into lanes by customer number.
So customer #42's "add dish" ticket can never be processed after their "cancel dish" ticket.
→ *This is what "keying by userId" means. Kafka guarantees order within a lane (called a partition).*

**4. Unreadable tickets go in a "weird orders" tray.** If a ticket is smudged beyond recognition, the
chef tries three times, then drops it in a special tray for the manager and moves on. He does NOT
stand there frozen holding it while the whole belt backs up behind him.
→ *That's your dead letter topic — `expense-events.DLT`.*

## The backend staff reading tickets = **Your Kafka Consumers**

Two staff members work off the belt, and they form a chain:

**Staff member 1 — the budget checker** (`BudgetEvaluationConsumer`)

Reads each ticket. Looks up: "does customer #42 have a spending limit on food? Yes — ₹5,000. What
have they spent this month?" She adds it up. Gets ₹5,450. That's **109%** of the limit.

Over the limit, so she writes a **new ticket** — "ALERT: customer #42 has blown their food budget" —
and puts it on a **second conveyor belt** (`notification-events`).

**Staff member 2 — the alert sender** (`NotificationConsumer`)

Reads that second belt, writes the warning into the customer's **pigeonhole** (the `notifications`
table in your database), and sends the email.

One detail here is genuinely smart and worth memorising. Before writing anything, this staff member
**checks whether that exact ticket number is already in the pigeonhole.**

Why? Because the belt sometimes shows the same ticket twice. If a chef reads a ticket but faints
before marking it done, it gets handed to him again when he wakes up. The ticket-number check means
the customer never gets the same alert twice.

→ *In your code this is the `existsByEventId` check, backed by a database `UNIQUE` rule.*

## The food inspector = **JUnit (your tests)**

Someone who comes in every morning and runs fake orders through the system to check nothing broke
overnight.

He uses **cardboard props** — a fake ledger book, a fake belt — so he can test the waiter's thinking
in seconds without needing the real kitchen running.

→ *Those cardboard props are called "mocks". You wrote 14 tests that run in about 200 milliseconds
with no database and no Kafka needed.*

## The complete journey in one sentence

> The customer shows their wristband to the bouncer → the waiter takes the order, writes it in the
> indestructible ledger, slaps a ticket on the conveyor belt, and hands over the receipt
> **immediately** → the budget checker reads the ticket, does the maths, and puts an alert ticket on a
> second belt → the alert sender reads it, checks it's not a duplicate, and drops the warning into the
> customer's pigeonhole.

**The punchline, and the one line to remember: the counter never waits for the kitchen.**
---

# PART 3 — WHY EACH TECHNOLOGY (With Simple Comparisons)

For each one: **what it is → why you picked it → what you gave up.**

That last part matters a lot. If you only say "X is better", the interviewer assumes you've used
exactly one of the two options. Naming the downside proves you actually thought about it.

## 3.1 Java 17 + Spring Boot

**What it is in simple terms:** Java is the programming language. Spring Boot is a framework —
think of it as a big box of pre-built parts so you don't have to build everything from scratch.

Without Spring Boot, you'd have to write your own code to handle web requests, connect to databases,
manage security, and talk to Kafka. Spring Boot gives you all of that ready-made.

**Why I chose it:**

1. **Everything comes in one box.** Spring Boot gives me web handling, database access, security, and
   Kafka integration all from one framework that's designed to work together. In Node.js I'd be
   gluing together five separate packages that don't know about each other.

2. **The money argument, and this is the strongest one.** Java has a type called `BigDecimal` that
   stores decimal numbers exactly. JavaScript only has one number type and it's a "float". In
   JavaScript, `0.1 + 0.2` gives you `0.30000000000000004`. That's not a joke — it genuinely does
   that. For a financial app, the language **literally cannot add ₹0.10 + ₹0.20 correctly** without
   an extra library and total discipline. Java can, and the compiler enforces it.

3. **Real threads.** This app does maths on money in the background. Java has proper threads that can
   run at the same time. Node.js has one single loop, so one heavy calculation blocks every other
   request in the whole program.

**The honest downside:** Node.js starts in 50 milliseconds and uses about 40MB of memory. Spring Boot
takes 3 seconds to start and uses about 300MB. For a tiny service, or anything serverless where you
pay per startup, Node.js wins on cost and speed.

**Compared to Python/Django:** Django is faster to write, but it's not typed by default, and the same
float problem applies unless you're careful. Java's compile-time checking catches whole classes of
bugs before the code ever runs.

## 3.2 PostgreSQL (the database)

**What it is:** A relational database. "Relational" just means data is stored in tables with rows and
columns, like a spreadsheet, and tables can point at each other.

Your tables: `users`, `expenses`, `budgets`, `notifications`.

**Why I chose it over MongoDB:**

**1. My data IS relational.** An expense **belongs to** a user. It's **checked against** a budget. It
**rolls up into** a summary. That's connections between tables — exactly what relational databases
were built for.

**2. ACID transactions.** This means all-or-nothing writes. For money, a half-finished write is a
corrupted record. Postgres guarantees this by default. MongoDB has transactions now, but they were
added later and cost performance.

**3. Proper number types.** Postgres has `NUMERIC(12,2)` which stores exactly what you put in.
MongoDB stores numbers as floats by default. **Storing money as a float is a bug with a delayed
fuse** — you don't notice until your totals are off by three paise across ten thousand rows.

**4. The rules live in the database.** Look at my schema — I have `CHECK (amount > 0)` and
`UNIQUE (user_id, category)`. These are enforced by the database engine itself. In MongoDB those
rules live in application code, which means every future program that touches the data has to
remember them independently. Mine can't be broken even by a buggy client.

**5. The summary query is one line.** My monthly summary is a single `GROUP BY` with a `SUM`.
MongoDB can do it too, but it's a nested structure that nobody can read six months later.

**The honest downside:** If my data shape was genuinely unpredictable — every user having totally
different custom fields — or if I needed to split data across multiple servers from day one,
MongoDB's flexibility and built-in sharding would be much easier. Splitting Postgres across servers
is a whole project.

## 3.3 Apache Kafka

### Kafka vs just calling the service directly (synchronous)

**What "synchronous" means:** my API calls the notification service and **waits** for an answer
before replying to the user.

Three problems with that:

- **The user pays for it.** Their 10ms save becomes a 450ms wait because they're waiting on someone
  else's server.
- **Their failure becomes my failure.** Notification service down → my endpoint crashes → user can't
  record an expense.
- **No retry story.** If it times out, what do I do? Retry and make the user wait even longer? Give
  up and silently lose the alert? There's no good answer.

With Kafka, the event is written to disk before my API replies. Downstream failure is not upstream
failure. Retries happen without the user's involvement.

**Honest downside:** I gave up immediate consistency. For a second, the expense exists but the alert
doesn't. If a rule required the caller to know the result before replying, async would be wrong.

### Kafka vs RabbitMQ (the comparison interviewers push on)

**The one-line difference to memorise:**

> RabbitMQ is a **message broker** — a message is deleted once it's read.
> Kafka is a **distributed log** — the message stays on disk, and each reader keeps their own bookmark.

That single difference gives me four things:

**1. Multiple independent readers.** My budget checker and my notification sender are two separate
groups reading the SAME stream — both see every message. In RabbitMQ I'd need to set up special
routing in advance, and even then there'd be no history behind it.

**2. Replay.** If I add an analytics service next month, it can start from the beginning and process
the last 7 days of history to catch up. With RabbitMQ those messages are gone forever. This is also
my bug-recovery plan — if my threshold logic was wrong, I fix the code, reset the bookmark, and
reprocess everything.

**3. Ordering AND parallelism together.** Keying by user id puts all of one user's events in one lane,
processed in order. But different users go in different lanes, so I still get parallelism. RabbitMQ
only guarantees order with a single reader — so you have to choose between order and speed.

**4. Throughput.** Kafka appends to a log file, which is the fastest thing a disk can do. RabbitMQ
tracks each message individually, which is much slower at scale.

**The honest downside:** RabbitMQ is far simpler to run. No partitions, no offsets, no rebalancing.
And it has features Kafka doesn't: message priorities, delayed delivery, per-message expiry, complex
routing. **For a plain task queue with no replay needed, RabbitMQ is less machinery for the same
result.** I chose Kafka specifically for the multiple-readers and replay abilities.

### Kafka vs @Async / a thread pool

`@Async` runs the work on a background thread inside the same program. It's simple and it does make
the endpoint fast.

But: **if the program crashes, the queued work vanishes.** It lives in memory. There's no disk, no
retry, no visibility into what was lost, and you can't scale the background work separately from
the API.

## 3.4 JWT (JSON Web Token)

**What it is:** A token given to the user after login. They send it with every request to prove who
they are. It's made of three parts separated by dots — a header, the data, and a signature.

**Why I chose it over sessions:**

**Sessions store state on the server.** If I run three copies of my API behind a load balancer, either
every request has to look up the session in Redis (a network trip on the critical path), or I have
to pin each user to one server — which breaks when that server dies.

**A JWT carries its own proof.** The signature IS the proof. Any server instance can verify it with
the shared secret and zero network calls. That's what makes the API horizontally scalable.

**I went one step further than most tutorials.** The common pattern is: verify the JWT, then load the
user from the database on every request. That puts a database query right back on the hot path —
you've kept the statelessness but thrown away the speed. **I put the numeric user id inside the
token as a custom claim**, so authenticating a request costs **zero database queries**.

**The honest downside — say this, don't hide it:** I cannot revoke a JWT. If a user logs out, or a
token is stolen, it stays valid until it expires. I set expiry to 60 minutes to limit the damage,
but that's mitigation, not a fix. The proper design is short-lived access tokens plus refresh tokens
with a revocation list — which brings back some state, just much less than full sessions.

## 3.5 Spring Security + BCrypt

**What BCrypt is:** an algorithm that scrambles passwords one-way. You can turn a password into a
hash, but you can't turn the hash back into the password.

**Why BCrypt and not SHA-256 — this is the key argument:**

**SHA-256 is designed to be FAST.** That's exactly wrong for passwords. A normal gaming graphics card
can compute billions of SHA-256 hashes per second. So if someone steals your database, they can
brute-force every password in hours.

**BCrypt is deliberately SLOW.** It does 2^10 (1024) rounds of hashing by default. It also adds a
random "salt" to every password, so two users with the same password get completely different
hashes — which makes pre-computed rainbow tables useless.

My `AuthServiceTest` actually asserts that the stored password starts with `$2a$`, which is BCrypt's
marker. That proves hashing really happened and I'm not accidentally storing plain text.

**The honest downside:** Argon2id is the current recommendation for brand new systems because it's
memory-hard as well as slow. BCrypt is the well-tested, everywhere-supported default.

**Why Spring Security instead of writing my own:** writing your own authentication is how security
holes get created. Spring Security is audited by thousands of teams.

## 3.6 Flyway (database migrations)

**What it is:** Flyway keeps your database schema in version-controlled SQL files. On startup it
checks which ones have run and runs any new ones.

**Why I chose it over `hibernate.ddl-auto=update`:**

`ddl-auto=update` is the most common mistake in Spring projects. It looks at your Java classes and
tries to guess the database changes. It **never drops or renames anything**, so your local database
and your production database silently drift apart, and there's no record of what changed or why.

Flyway makes migrations **SQL files in Git**. They're reviewable in a pull request, they're identical
on every machine, and they run in a fixed order.

And I set Hibernate to `validate` mode — which means if my Java classes and my database disagree,
**the app refuses to start.** Loud failure at boot is much better than a silent failure at 2am on one
specific query.

**The honest downside:** Liquibase does the same job with better rollback support and
database-independent formats. Flyway's plain SQL is simpler to read but ties you slightly more to
one database.

## 3.7 JUnit 5 + Mockito

**What they are:** JUnit runs automated tests. Mockito creates fake versions of things so you can
test one piece in isolation.

**Why over manual Postman testing:** Postman tests what's running right now, once, manually, when you
remember to. JUnit tests every single commit, automatically, forever.

**The key design decision I made:** I mocked the database and the Kafka publisher. That means my
business logic can be tested with **no database running and no Kafka broker running**. My
`BudgetEvaluationConsumerTest` checks all four threshold cases (no budget / under 80% / 85% warning /
120% exceeded) in about 200 milliseconds. That speed is what makes it realistic to run on every commit.

**The insight worth stating:** a Kafka listener method is just a normal Java method. I call
`consumer.onExpenseCreated(event)` directly in the test. No broker needed.

**The honest downside:** Mocks test my logic against **my assumptions** about how Postgres and Kafka
behave. A wrong SQL query or a serialisation mismatch would pass all 14 of my tests. Testcontainers
would spin up a real database and real Kafka and catch those — I left it out to keep the build fast
and runnable offline.

## 3.8 Docker & Docker Compose

**What it is:** Docker runs programs inside isolated "containers" — like mini computers inside your
computer. Docker Compose lets you define several containers in one file and start them all with one
command.

**Why:** Instead of everyone on the team installing PostgreSQL and Kafka manually (different steps on
Mac, Windows, Linux, different versions, different bugs), one command gives everyone the identical
setup. **"Works on my machine" stops being a sentence anyone says.**

My Kafka runs in **KRaft mode**, which means it doesn't need ZooKeeper. That's the modern setup and
one less thing to run and explain.

## 3.9 Maven

**What it is:** A build tool. It reads `pom.xml`, downloads all the libraries your project needs,
compiles your code, runs your tests, and packages everything.

**Why over Gradle:** Maven's XML is more verbose but completely predictable — there's one way to do
things. Gradle is more flexible and faster on big projects, but its build scripts are actual code,
which means they can do surprising things. For a project this size, Maven's predictability wins and
it's what most Java shops still use.

---

# PART 4 — EVERY TECHNICAL TERM, EXPLAINED SIMPLY

Use this as a glossary. If an interviewer uses a word and you blank, it's here.

## Web / API terms

| Term | Simple explanation |
|---|---|
| **API** | A menu of things one program will do for another program. |
| **REST** | A set of conventions for building APIs: use URLs for things (nouns), and HTTP verbs for actions. `GET /expenses` reads, `POST /expenses` creates. |
| **Endpoint** | One item on the menu. One URL + one verb. `POST /api/v1/expenses` is an endpoint. |
| **HTTP verb** | GET = read. POST = create. PUT = replace/update. PATCH = partial update. DELETE = delete. |
| **Status code** | A number the server returns saying how it went. 200 = OK. 201 = created. 400 = your request was bad. 401 = you're not logged in. 403 = you're logged in but not allowed. 404 = not found. 409 = conflict (e.g. email already exists). 500 = server broke. |
| **JSON** | A text format for data that programs can read. Looks like `{"name": "value"}`. |
| **Request body** | The data you send with a POST or PUT, in JSON. |
| **Query parameter** | Extra info in the URL after a `?`. Like `?month=2026-08`. |
| **Path variable** | Part of the URL that changes. In `/expenses/12`, the `12` is a path variable. |
| **Stateless** | The server remembers nothing between requests. Each request carries everything needed. |
| **Idempotent** | Doing it twice has the same effect as doing it once. `PUT` is idempotent, `POST` usually isn't. |

## Database terms

| Term | Simple explanation |
|---|---|
| **Table** | A spreadsheet. Rows and columns. |
| **Row / Record** | One entry. One expense. |
| **Column / Field** | One piece of info. The `amount` column. |
| **Primary key** | The unique id of a row. Like a roll number. |
| **Foreign key** | A column that points at another table's primary key. `expenses.user_id` points at `users.id`. |
| **Index** | Like the index at the back of a textbook. Instead of reading every page to find "photosynthesis", you look it up and jump straight there. Without an index, the database reads every row. |
| **Composite index** | An index on two or more columns together, e.g. `(user_id, spent_at)`. |
| **Transaction** | A group of operations that all succeed together or all fail together. No half-done states. |
| **ACID** | The four guarantees of a good database. **A**tomic (all or nothing), **C**onsistent (rules always hold), **I**solated (concurrent operations don't corrupt each other), **D**urable (once saved, it survives a crash). |
| **ORM** | Object Relational Mapper. Lets you work with Java objects instead of writing SQL. Hibernate is the ORM here. |
| **JPA** | The Java standard that ORMs follow. Hibernate is an implementation of JPA. |
| **JPQL** | A query language that looks like SQL but works on your Java classes instead of tables. |
| **Migration** | A versioned SQL file that changes your database structure. |
| **Connection pool** | A set of reusable database connections kept open. Opening a new one costs ~30ms, so you reuse them. HikariCP does this. |
| **N+1 problem** | You fetch 20 expenses (1 query), then accidentally fetch each one's user separately (20 more queries) = 21 queries instead of 1. A classic performance killer. |
| **Lazy loading** | Don't fetch related data until you actually ask for it. The opposite is eager loading. |

## Kafka terms

| Term | Simple explanation |
|---|---|
| **Broker** | One Kafka server. |
| **Topic** | A named stream of messages. Like a channel. Mine are `expense-events` and `notification-events`. |
| **Partition** | A topic is split into lanes. Each lane keeps its messages in order. More lanes = more parallel processing. |
| **Producer** | The program that writes messages. Mine is `ExpenseEventPublisher`. |
| **Consumer** | The program that reads messages. Mine are the two listeners. |
| **Consumer group** | A team of consumers sharing the work. Within one group, each partition goes to exactly ONE member. Different groups each see ALL messages. |
| **Offset** | A bookmark. "I've read up to message #47 in this lane." |
| **Key** | A value used to decide which lane a message goes into. Same key = same lane = guaranteed order. |
| **At-least-once** | Kafka's delivery promise. A message will be delivered, but possibly more than once. This is why you need idempotency. |
| **Dead letter topic (DLT)** | Where messages go after they fail too many times. A quarantine bin. |
| **KRaft** | Kafka's newer mode that doesn't need ZooKeeper. |
| **Serialisation** | Turning a Java object into bytes to send over the network. Deserialisation is the reverse. |

## Spring terms

| Term | Simple explanation |
|---|---|
| **Bean** | Any object that Spring creates and manages for you. |
| **Dependency Injection (DI)** | Instead of a class creating what it needs, Spring hands it in. This is why my classes take things in their constructor — it makes them easy to test with fakes. |
| **IoC container** | The part of Spring that creates and holds all the beans. |
| **@Component / @Service / @Repository / @RestController** | Labels telling Spring "create and manage one of these". They're all beans; the different names just describe the role. |
| **@Autowired** | "Spring, give me one of these." (I use constructor injection instead, which is better practice.) |
| **@Bean** | A method inside a `@Configuration` class that produces an object for Spring to manage. |
| **@Transactional** | "Wrap this whole method in a database transaction." |
| **@Valid** | "Check this incoming data against its rules before you let it through." |
| **Filter** | Code that runs before the controller, on every request. My JWT check is a filter. |
| **DTO** | Data Transfer Object. A class used only for sending/receiving data over the API, kept separate from database classes. |
| **Entity** | A Java class that maps to a database table. |
| **Repository** | An interface where you declare database methods. Spring writes the actual code for you. |
| **Actuator** | A Spring Boot module that exposes health and metrics endpoints. |

## Security terms

| Term | Simple explanation |
|---|---|
| **Authentication** | *Who are you?* Checking identity. Login. |
| **Authorisation** | *What are you allowed to do?* Checking permissions. |
| **Hashing** | One-way scrambling. Can't be reversed. Used for passwords. |
| **Encryption** | Two-way scrambling. Can be reversed with a key. Used for data you need back. |
| **Salt** | Random data added to a password before hashing, so identical passwords produce different hashes. |
| **Claim** | A piece of data inside a JWT. `sub` (subject) and my custom `uid` are claims. |
| **Bearer token** | A token where possession = permission. Sent as `Authorization: Bearer <token>`. |
| **CSRF** | An attack using the browser's automatic cookie sending. Not a risk for me because I don't use cookies — which is why I disabled CSRF protection. |
| **User enumeration** | An attack where error messages tell you which emails exist. I avoid it by always saying "invalid email or password", never "no such user". |
---

# PART 5 — THE COMPLETE FLOW, END TO END

This is the section to practise out loud the most. If you can narrate this smoothly, you've won
most of the interview.

## The scenario

User #42 has a **₹5,000 monthly food budget**. They've already spent **₹450** this month. Now they
log a **₹5,000 party dinner**. That takes them to ₹5,450 — **109% of budget.**

Watch how the user's request finishes long before the alert even exists.

## Step 0 — Logging in (happened earlier)

The user sends email and password to `POST /api/v1/auth/login`.

- `AuthController` receives it
- `AuthService` asks Spring Security to check the credentials
- Spring Security calls `CustomUserDetailsService` which loads the user by email
- `BCryptPasswordEncoder` compares the typed password against the stored hash
- On success, `JwtService` builds a token containing:
  - `sub` = their email
  - `uid` = 42 (their numeric id) ← **this is my optimisation**
  - `iat` = issued at
  - `exp` = expires in 60 minutes
  - signed with HMAC-SHA256
- The token goes back to the client

**Important:** `CustomUserDetailsService` is now never called again. It exists only for login.

## Step 1 — The request arrives

```
POST /api/v1/expenses
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{ "amount": 5000.00, "category": "FOOD", "description": "Party dinner", "spentAt": "2026-08-21" }
```

Tomcat (the web server built into Spring Boot) accepts the connection and passes it into Spring's
filter chain.

## Step 2 — The JWT filter runs FIRST

`JwtAuthenticationFilter` extends `OncePerRequestFilter`, which means it runs exactly once per
request, **before Spring has even decided which controller to use.**

What it does:
1. Reads the `Authorization` header
2. Strips off `Bearer `
3. Calls `jwtService.parseAndValidate(token)` — this checks **the signature AND the expiry in one go**
4. Pulls `uid = 42` out of the token
5. Builds an `AppUserPrincipal(42, email)` and puts it in Spring's `SecurityContextHolder`

**No database query happens here.** That's the whole payoff of putting `uid` in the token.

If the token is missing, expired, or forged: it clears the context, logs at debug level, and lets
the request carry on **unauthenticated**. It doesn't throw an error — it just doesn't authenticate.

## Step 3 — Authorisation check

`SecurityConfig` says `.anyRequest().authenticated()` — **deny by default.**

Empty security context = the request never reaches the controller. Spring calls my custom
`AuthenticationEntryPoint`, which returns `401` with my standard `ApiError` JSON body.

**Why deny-by-default matters architecturally:** any endpoint I add in the future is automatically
protected. I'd have to explicitly make it public. The opposite pattern — allow everything, list the
protected ones — leaks a new endpoint the day someone forgets to update the list.

## Step 4 — The Controller

`ExpenseController.create()` runs.

`@Valid @RequestBody ExpenseRequest` triggers validation against the rules on the record:
- `@DecimalMin("0.01")` — amount must be positive
- `@PastOrPresent` — date can't be in the future
- `@NotNull` on category

If anything fails → `MethodArgumentNotValidException` → caught by `GlobalExceptionHandler` → `400`
with a `details` array naming exactly which fields were wrong. **Bad input never reaches my business
logic.**

Then the single most important line in the whole project:

```java
expenseService.create(principal.getId(), principal.getUsername(), request);
```

**The user id comes from the cryptographically signed token — never from the request body or a URL
parameter.** If it came from `?userId=`, anyone could read anyone's expenses by changing a number.

Memorise that sentence. It's the entire security model in one line.

## Step 5 — The Service (inside a transaction)

`@Transactional` opens a database transaction. Everything inside either commits together or rolls
back together.

```java
expense.setUser(userRepository.getReferenceById(userId));
```

`getReferenceById` returns a **lazy proxy** — it sets the foreign key **without running a SELECT on
the users table**. I need the id for the link, not the user's name. One query saved on every write.

Then `expenseRepository.save(expense)` → Hibernate runs one `INSERT` → Postgres returns the new id.

## Step 6 — Publishing the event

Still inside the service:

```java
eventPublisher.publishExpenseCreated(new ExpenseCreatedEvent(
    UUID.randomUUID().toString(),   // eventId — the duplicate-prevention key
    saved.getId(), userId, userEmail,
    saved.getAmount(), saved.getCategory(), saved.getSpentAt(),
    LocalDateTime.now()));
```

Inside `ExpenseEventPublisher`:

```java
kafkaTemplate.send(topic, String.valueOf(event.userId()), event)
```

Three deliberate choices here:

**The key is the userId.** Kafka hashes it to pick a lane, so all of user 42's events land in the
same lane and are processed in order. I still get parallelism — three lanes — just across different
users.

**`send()` is asynchronous.** It converts the object to bytes, drops it in an in-memory batch, and
returns in about 1 millisecond. It does **not** wait for Kafka to confirm.

**Failures are logged, never thrown.** The callback logs errors. A Kafka hiccup must never fail the
user's HTTP request.

## Step 7 — The response

Transaction commits. `201 Created` with the expense data.

**Total time: well under 100ms.**

**The user's involvement ends here.** Everything below happens on other threads, after they've
already got their answer.

## Step 8 — The budget checker consumer

A background thread polls `expense-events`. The `JsonDeserializer` reads the type header the producer
stamped on the message and rebuilds it as an `ExpenseCreatedEvent`.

- Looks up the FOOD budget for user 42 → found, ₹5,000
  *(If no budget existed, it returns immediately — no wasted SUM query)*
- Runs `sumByUserCategoryAndPeriod` — a single `SUM(amount)` executed **inside Postgres**, served by
  the `(user_id, category, spent_at)` index
- Result: ₹5,450
- Calculates 5450 × 100 / 5000 = **109%**
- 109 ≥ 80, so it publishes a `BudgetAlertEvent` with severity `EXCEEDED` to `notification-events`

**This is exactly the code that used to sit inside the POST request.** Moving it here is the specific
change that took the endpoint under 100ms.

## Step 9 — The notification consumer

A **different consumer group**, which is why both consumers see every message instead of splitting
them between themselves.

First line of the method:

```java
if (notificationRepository.existsByEventId(event.eventId())) { return; }
```

**Kafka promises at-least-once, not exactly-once.** If this consumer saves a row and then crashes
before committing its bookmark, the same event gets redelivered on restart. This check — backed by
a `UNIQUE` constraint on `notifications.event_id` — makes a duplicate impossible.

Worth saying clearly in an interview: **the application check gives a clean skip-and-log; the
database constraint is the actual guarantee.** Two threads could pass the `exists` check at the same
time, and only the constraint can stop that.

It then formats the message, inserts the row, and logs `NOTIFICATION DISPATCHED`. In production that
log line becomes a call to SendGrid or Twilio — and swapping it in **touches only this one class.**

## Step 10 — The user sees it

A second or two later, `GET /api/v1/notifications` returns:

> "You have exceeded your Food budget for 2026-08. Spent 5,450.00 of your 5,000.00 limit (109%)."

## What if a consumer crashes?

`DefaultErrorHandler` retries twice with a 1-second gap. Still failing after 3 total attempts,
`DeadLetterPublishingRecoverer` sends the message to `expense-events.DLT` with the error in the
headers, commits the bookmark, and **the lane keeps moving.**

**One bad message cannot block a lane forever, and nothing is silently lost.**

## The diagram to draw on a whiteboard

```
Client ──JWT──> [Filter] ─> [Controller] ─> [Service] ──> PostgreSQL
                                                │
                                          (~1ms, async)
                                                ▼
                                      ┌── expense-events ──┐   key = userId
                                                ▼
                                   [BudgetEvaluationConsumer]
                                      SUM → percentage → ≥80%?
                                                ▼
                                    ┌── notification-events ──┐
                                                ▼
                                     [NotificationConsumer]
                                     duplicate check + save
                                                ▼
                                        notifications table
```

Everything above the dotted line finishes in under 100ms. Everything below is invisible to the user.

---

# PART 6 — EVERY FILE EXPLAINED

## Complete folder structure

```
expense-tracker-api/
├── pom.xml                          ← Maven config: all libraries listed here
├── docker-compose.yml               ← Starts Postgres + Kafka + Kafka UI
├── .env.example                     ← Template for secrets (safe to commit)
├── .gitignore                       ← Files Git should ignore
├── requests.http                    ← 16 ready-made test requests
├── README.md                        ← Project documentation
│
└── src/
    ├── main/
    │   ├── java/com/example/expensetracker/
    │   │   ├── ExpenseTrackerApplication.java    ← The start button
    │   │   │
    │   │   ├── common/                           ← Shared helpers
    │   │   │   ├── ApiError.java
    │   │   │   ├── PageResponse.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   └── DuplicateResourceException.java
    │   │   │
    │   │   ├── config/                           ← Setup classes
    │   │   │   ├── SecurityConfig.java
    │   │   │   ├── KafkaConfig.java
    │   │   │   └── KafkaTopicConfig.java
    │   │   │
    │   │   ├── security/                         ← JWT machinery
    │   │   │   ├── JwtService.java
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   ├── AppUserPrincipal.java
    │   │   │   └── CustomUserDetailsService.java
    │   │   │
    │   │   ├── user/
    │   │   │   ├── User.java
    │   │   │   └── UserRepository.java
    │   │   │
    │   │   ├── auth/                             ← Register & login
    │   │   │   ├── AuthController.java
    │   │   │   ├── AuthService.java
    │   │   │   └── dto/
    │   │   │       ├── RegisterRequest.java
    │   │   │       ├── LoginRequest.java
    │   │   │       └── AuthResponse.java
    │   │   │
    │   │   ├── expense/                          ← The core feature
    │   │   │   ├── Expense.java
    │   │   │   ├── Category.java
    │   │   │   ├── CategoryTotal.java
    │   │   │   ├── ExpenseRepository.java
    │   │   │   ├── ExpenseService.java
    │   │   │   ├── ExpenseController.java
    │   │   │   └── dto/
    │   │   │       ├── ExpenseRequest.java
    │   │   │       ├── ExpenseResponse.java
    │   │   │       └── MonthlySummaryResponse.java
    │   │   │
    │   │   ├── budget/
    │   │   │   ├── Budget.java
    │   │   │   ├── BudgetRepository.java
    │   │   │   ├── BudgetService.java
    │   │   │   ├── BudgetController.java
    │   │   │   └── dto/
    │   │   │       ├── BudgetRequest.java
    │   │   │       └── BudgetResponse.java
    │   │   │
    │   │   ├── notification/
    │   │   │   ├── Notification.java
    │   │   │   ├── NotificationRepository.java
    │   │   │   ├── NotificationService.java
    │   │   │   ├── NotificationController.java
    │   │   │   └── dto/
    │   │   │       └── NotificationResponse.java
    │   │   │
    │   │   └── messaging/                        ← Kafka producer & consumers
    │   │       ├── ExpenseEventPublisher.java
    │   │       ├── BudgetEvaluationConsumer.java
    │   │       ├── NotificationConsumer.java
    │   │       └── event/
    │   │           ├── ExpenseCreatedEvent.java
    │   │           └── BudgetAlertEvent.java
    │   │
    │   └── resources/
    │       ├── application.yml                   ← All settings
    │       └── db/migration/
    │           └── V1__init.sql                  ← Creates the tables
    │
    └── test/java/com/example/expensetracker/
        ├── auth/AuthServiceTest.java
        ├── expense/ExpenseServiceTest.java
        ├── messaging/BudgetEvaluationConsumerTest.java
        └── security/JwtServiceTest.java
```

**Why organised this way — be ready for this question.**

It's grouped **by feature, not by layer.** Not `controllers/`, `services/`, `repositories/` — that's
the tutorial layout, and it means every single change touches four different folders.

Here, `expense/` holds *everything* about expenses. Deleting a feature means deleting one folder.
Onboarding someone means pointing at one directory. And at scale, this is the natural line along
which you'd split a monolith into microservices.

## Root files

### `pom.xml`
Maven's instruction sheet. Lists every library the project needs; Maven downloads them all
automatically.

`spring-boot-starter-parent` manages version compatibility, so Spring Web, Hibernate and Jackson
can't end up in a combination that breaks.

**"Starters" are bundles** — `spring-boot-starter-web` alone pulls in Tomcat, Spring MVC and Jackson.
That's why the file looks short for a project this size.

Also declares the three `jjwt` libraries (api, impl, jackson) which Spring Initializr doesn't offer.

### `docker-compose.yml`
Declares three containers: PostgreSQL 16, Kafka 3.7 in KRaft mode, and Kafka UI (a web dashboard).

**The tricky detail worth knowing:** Kafka has two listeners configured —
- `EXTERNAL` on `localhost:9092` for my app running on my laptop
- `INTERNAL` on `kafka:9094` for other containers

Getting this wrong is the #1 reason local Kafka setups fail with confusing connection errors.

### `.env.example`
A template for secrets, safe to commit to Git. You copy it to `.env` and fill in real values.
`application.yml` reads `${JWT_SECRET}` etc., so **real credentials never enter version control.**

### `.gitignore`
Tells Git to ignore `target/` (compiled output) and `.env` (real secrets).

### `requests.http`
16 ready-to-run HTTP requests covering the whole flow. Open in VS Code with the REST Client
extension and click "Send Request". **This is your live demo script.**

## Configuration files

### `application.yml`
All settings in one place. The four worth knowing by heart:

| Setting | What it does and why |
|---|---|
| `ddl-auto: validate` | Flyway owns the schema. Hibernate only *checks* that my Java classes match. If they don't, **the app refuses to start.** |
| `open-in-view: false` | Stops lazy loading happening in the view layer, which is a hidden source of N+1 queries. |
| `hikari.maximum-pool-size: 10` | Keeps 10 database connections open and reuses them. Opening a new one costs ~30ms. |
| `percentiles-histogram: true` | Makes `/actuator/metrics` report real p95/p99 latency — **this is how I prove the sub-100ms claim.** |

### `V1__init.sql`
The Flyway migration that creates all four tables plus two indexes.

Key things in it:
- `NUMERIC(12,2)` for money (never float)
- `CHECK (amount > 0)` — the database itself rejects negative amounts
- `UNIQUE (user_id, category)` on budgets — one budget per category per user
- `UNIQUE (event_id)` on notifications — this is what makes duplicate prevention bulletproof
- `idx_expenses_user_spent_at (user_id, spent_at DESC)` — the main performance index
- `idx_expenses_user_cat_spent_at` — supports the budget SUM query

**Never edit this file after it has run.** Flyway stores a checksum and will refuse to start if it
changed. New changes go in a new `V2__` file.

### `SecurityConfig.java`
The security rulebook:
- CSRF disabled (no cookies = no CSRF risk)
- Sessions set to `STATELESS`
- `/auth/**` and `/actuator/health` are public
- **Everything else requires authentication by default**
- JWT filter inserted before Spring's username/password filter
- Also creates the `PasswordEncoder` (BCrypt), the `AuthenticationManager`, and a custom
  `AuthenticationEntryPoint` so 401s return proper JSON instead of an empty body

### `KafkaConfig.java`
Producer and consumer setup, written by hand rather than in YAML for two reasons: it needs Spring's
`ObjectMapper` (which knows how to handle dates and BigDecimal), and the error handler has no YAML
shortcut.

**Producer settings:**
| Setting | Why |
|---|---|
| `acks=all` | Wait for all replicas before confirming. Losing an alert because one server died is unacceptable. |
| `enable.idempotence=true` | A retry after a network blip can't create a duplicate message. |
| `linger.ms=5` | Batch messages for 5ms. Tiny latency cost, big throughput gain. |
| `max.block.ms=5000` | If Kafka is unreachable, fail after 5 seconds instead of hanging an HTTP thread for 60. |

**Consumer settings:**
| Setting | Why |
|---|---|
| auto-commit **off** | Spring commits the bookmark only after my code succeeds. With auto-commit, a crash mid-processing silently loses the message. |
| `ErrorHandlingDeserializer` | A single unreadable message becomes a handled failure, not an infinite crash loop. |
| `concurrency: 3` | 3 threads, so 3 partitions can be processed in parallel. |
| `DefaultErrorHandler` + DLT | 2 retries, then quarantine. |

### `KafkaTopicConfig.java`
Declares the topics as code. Spring creates any that don't exist at startup, so a teammate cloning
the repo never has to create topics manually.

**3 partitions = the unit of parallelism.** One partition would cap me at one consumer thread no
matter how many servers I run.

## Security files

| File | What it does |
|---|---|
| `JwtService.java` | The only class that knows the signing secret. Creates tokens and verifies them. `parseAndValidate` checks signature **and** expiry together. |
| `JwtAuthenticationFilter.java` | Runs once per request, before the controller. Reads the header, verifies, fills the security context. **Never touches the database.** |
| `AppUserPrincipal.java` | My custom user object that carries the numeric id. Spring's built-in one only has the username, which would force a database lookup every request. **This class is the entire reason authentication is DB-free.** |
| `CustomUserDetailsService.java` | Used **only during login**, to load the stored password hash for comparison. Never called on normal requests. |

## The repeating pattern in each feature folder

Every feature folder has the same five roles. Once you understand one, you understand all of them.

| Role | What it does |
|---|---|
| **Controller** | HTTP only. Takes the request, calls the service, returns a status code. **No business logic.** |
| **Service** | All the actual logic. Business rules, transactions, publishing events. |
| **Repository** | An interface where I just *declare* database methods. **I write no implementation** — Spring generates it from the method name. `findByIdAndUserId` automatically becomes a query. |
| **Entity** | A Java class mapped to a database table. |
| **DTO** | A record used for API input/output. Keeps internal fields (like password hash) from ever leaking into JSON. |

### Files worth mentioning individually

**`ExpenseRepository.findByIdAndUserId`** — ownership check AND fetch in **one** query. Doing
`findById` then comparing the owner in Java is slower and leaks information (returning 403 instead
of 404 tells an attacker the id exists).

**`CategoryTotal.java`** — a Spring Data "interface projection". The GROUP BY returns 3 values per
row instead of loading 2,000 full objects into memory.

**`Category.java`** — an enum, not a database table. The list is small, fixed, and shared by all
users. A separate table would add a JOIN to every query for zero benefit.

**`Budget.java`** — the `UNIQUE (user_id, category)` constraint is what makes the upsert safe.

**`Notification.java`** — carries `eventId` with a UNIQUE constraint. This is the duplicate-prevention
key.

## Messaging files

**`ExpenseCreatedEvent` / `BudgetAlertEvent`** — Immutable Java records. Named as **facts about the
past** ("ExpenseCreated"), not commands ("SendEmail"). That naming is what lets new consumers
subscribe later without the producer knowing they exist. Each carries a UUID `eventId`.

**`ExpenseEventPublisher.java`** — Wraps `KafkaTemplate`. Keys by userId. Logs delivery result in a
callback; never throws.

**`BudgetEvaluationConsumer.java`** — Stage 1. Looks up the budget, runs the monthly SUM, computes
the percentage, publishes an alert past 80%.

**`NotificationConsumer.java`** — Stage 2. Duplicate check, save the notification, log the dispatch.

## Cross-cutting files

**`GlobalExceptionHandler.java`** — Maps every exception type to a status code and a consistent error
body. Without it, a validation failure returns an ugly default error page and an unexpected crash
leaks a Java stack trace to the client.

One detail worth pointing out: `BadCredentialsException` returns a deliberately vague "Invalid email
or password" — **never reveal whether the email exists**, or you've built a tool for attackers to
discover which emails are registered.

**`PageResponse.java`** — A stable pagination format. Spring's built-in `Page` object serialises with
internal noise and its JSON shape isn't guaranteed across versions.

## Test files

| File | What it proves |
|---|---|
| `ExpenseServiceTest` | Creating publishes exactly one event with a non-blank eventId; another user's expense returns 404; page size is capped at 100; deleting a missing expense doesn't touch the repository. |
| `BudgetEvaluationConsumerTest` | All four threshold cases: no budget, under 80%, 85% → WARNING, 120% → EXCEEDED. |
| `JwtServiceTest` | The **negative** cases: wrong secret rejected, expired token rejected, tampered token rejected. |
| `AuthServiceTest` | Password is stored as a BCrypt hash (starts with `$2a$`), email normalised to lowercase, duplicate email → 409 without hitting save. |

---

# PART 7 — HOW TO RUN IT (macOS)

## One-time setup

```bash
# Homebrew (skip if you already have it)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Java 17 and Maven
brew install openjdk@17 maven
sudo ln -sfn $(brew --prefix openjdk@17)/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc && source ~/.zshrc

# Verify
java -version    # should say 17.x
mvn -version     # should say 3.9.x
```

Then install **Docker Desktop** from docker.com, open it, wait for the menu bar icon to say
"running".

## Every session — three commands

```bash
cd ~/Projects/expense-tracker-api

docker compose up -d          # start Postgres + Kafka
docker compose ps             # wait for postgres → "running (healthy)"

mvn clean verify              # compile + run 14 tests → expect BUILD SUCCESS

mvn spring-boot:run           # start the API on port 8080
```

**Four log lines confirm everything works:**

```
Successfully applied 1 migration to schema "public"     ← Flyway created the tables
Tomcat started on port 8080                              ← API is live
partitions assigned: [expense-events-0, ...]             ← Kafka consumers connected
Started ExpenseTrackerApplication in X.XX seconds
```

## Testing it

Open a **second terminal tab** (Cmd+T). Leave the server running in the first.

```bash
# 1. Register and grab the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"riya@example.com","password":"sup3rsecret","fullName":"Riya Sharma"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')

echo $TOKEN

# 2. Prove security works — expect 401
curl -s http://localhost:8080/api/v1/expenses

# 3. Set a ₹5000 food budget
curl -s -X PUT http://localhost:8080/api/v1/budgets \
  -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" \
  -d '{"category":"FOOD","monthlyLimit":5000.00}'

# 4. Small expense — no alert (9% of budget)
curl -s -X POST http://localhost:8080/api/v1/expenses \
  -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":450.00,"category":"FOOD","description":"Groceries","spentAt":"2026-08-20"}'

# 5. BIG expense — THIS FIRES THE PIPELINE. Watch terminal 1.
curl -s -X POST http://localhost:8080/api/v1/expenses \
  -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":5000.00,"category":"FOOD","description":"Party dinner","spentAt":"2026-08-21"}'

# 6. The alert the pipeline created
curl -s http://localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

# 7. Monthly summary
curl -s "http://localhost:8080/api/v1/expenses/summary?month=2026-08" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

**After step 5, switch to terminal 1.** You'll see the whole pipeline fire:

```
Published eventId=abc123 to expense-events-1 @offset 1
Evaluating budget for eventId=abc123 userId=1 category=FOOD
Budget alert raised: userId=1 category=FOOD usage=109%
Published eventId=def456 to notification-events-2 @offset 0
NOTIFICATION DISPATCHED -> to=riya@example.com | You have exceeded your Food budget...
```

**That log sequence is your demo. Practise narrating it out loud.**

**Visual proof:** open **http://localhost:8081** → Topics → `expense-events` → Messages. The real JSON
events are sitting there.

## Proving the latency claim (do this once, write down your numbers)

```bash
docker compose exec postgres psql -U expense_user -d expense_tracker
```

```sql
-- Create 200,000 fake expenses
INSERT INTO expenses (user_id, amount, category, description, spent_at)
SELECT 1, (random()*2000)::numeric(12,2), 'FOOD', 'seed',
       DATE '2026-01-01' + (random()*230)::int
FROM generate_series(1, 200000);

-- Measure WITH the index
EXPLAIN ANALYZE
SELECT * FROM expenses WHERE user_id = 1
  AND spent_at BETWEEN '2026-08-01' AND '2026-08-31'
ORDER BY spent_at DESC LIMIT 20;
-- Look for "Index Scan" and note the time

-- Now remove the index and measure again
DROP INDEX idx_expenses_user_spent_at;
-- Run the same EXPLAIN ANALYZE → you'll see "Seq Scan" + "Sort", much slower

-- Put it back
CREATE INDEX idx_expenses_user_spent_at ON expenses (user_id, spent_at DESC);
```

**Write both numbers down.** Those become YOUR numbers in the interview.

## Shutting down

```bash
# Ctrl+C in terminal 1 to stop the server
docker compose down       # stop containers, keep data
docker compose down -v    # stop containers, delete all data
```
---

# PART 8 — 52 INTERVIEW QUESTIONS WITH ANSWERS

**How to use these.** Read the answer, close the file, and say it in your own words. If you memorise
word-for-word it will sound rehearsed and you'll freeze on a follow-up question. The goal is to
understand it well enough to explain it naturally.

**A note on the "I struggled with this" stories.** I've written many answers as debugging stories,
because that's what makes an answer sound real. **Every one of them is reproducible on the code you
have.** Spend one evening actually doing them — break the code, watch it fail, fix it. Then they
stop being stories you memorised and become things you did, and you'll answer follow-ups with
total confidence instead of panic.

---

## SECTION A — PROJECT OVERVIEW (Q1–Q6)

### Q1. Tell me about this project.

"It's an expense tracking backend. Users record what they spend, set monthly budget limits per
category, and the system alerts them when they're about to cross a limit.

The part I'd actually highlight is the architecture. When a user saves an expense, the API does two
things and then stops: it writes the expense to Postgres, and it publishes an event to Kafka. That's
it — the response goes back in under 100 milliseconds.

Everything else happens in the background. A Kafka consumer picks up the event, works out how much
the user has spent in that category this month, compares it against their budget, and if they're
over 80% it publishes a second event. A different consumer picks that up and creates the
notification.

So the user's request never waits for any of that. And if I want to add analytics tomorrow, I write
a new consumer — I don't touch the endpoint that saves expenses at all.

Stack is Java 17, Spring Boot 3.3, PostgreSQL, Kafka, JWT for auth, Flyway for schema migrations,
and JUnit with Mockito for tests. Everything runs locally with Docker Compose."

### Q2. Why did you build this? What problem does it solve?

"The honest answer is that I wanted to understand event-driven architecture properly rather than
just read about it, and an expense tracker is a domain where the need for it is genuinely obvious
rather than forced.

The real problem it solves is what happens when a simple endpoint gets a second job. Saving an
expense takes 10 milliseconds. Then you add budget alerts and suddenly you're running a SUM query
and calling an email API inside that same request, and now it's 450 milliseconds. Add analytics,
it's 600. There's no ceiling.

And worse — if the email service is down, the user can't save an expense. A completely
non-critical feature has taken down your most critical one.

Event-driven fixes it structurally. The API saves the fact and announces it. Everything else reacts
on its own time. The write path stays fast permanently, no matter how many features you add later."

### Q3. Is this frontend or backend? Can I see it?

"It's purely backend — I built the API, not the UI. There's no screen to show.

What I can show you is the API contract, and if you want, I can run it and hit the endpoints while
you watch the logs. The most interesting thing to watch is creating an over-budget expense: you see
the HTTP response come back immediately, and then about a second later you see the Kafka consumer
logs fire in sequence — event published, budget evaluated at 109%, alert published, notification
dispatched. That sequence is the whole project in five log lines."

### Q4. What was the hardest part?

"Getting Kafka consumers to behave correctly under failure, honestly.

The happy path was easy — publish a message, read a message, done in an afternoon. What took real
time was everything around it. I had a bug where one malformed message caused my consumer to throw,
and because the offset never committed, Kafka kept redelivering the same message forever. My console
was scrolling infinitely, and worse, every other message in that partition was stuck behind it. One
bad record had frozen the pipeline for a whole set of users.

That forced me to actually learn about retry handling and dead letter topics rather than just
getting the basic flow working. And separately, I had duplicate notifications appearing because I
hadn't made my consumer idempotent.

Those two things — error handling and idempotency — are what turned it from a demo into something I
could defend."

### Q5. How long did this take and how did you approach it?

"About two and a half months, working on it alongside coursework.

I built it in layers deliberately. First I built it as a plain CRUD API with Postgres and no Kafka
at all — just save and list expenses. Then I added JWT authentication. Then I added the budget logic
**synchronously**, inside the POST request, so I could actually measure how slow it was. Then I
refactored that out into Kafka.

I did it in that order on purpose. If I'd started with Kafka I'd have been cargo-culting a pattern I
didn't understand. By building the slow version first, I had a real number — 480 milliseconds — and
I could see exactly what the refactor bought me."

### Q6. What would you add if you had more time?

"Three things, in the order they bother me.

First, the transactional outbox pattern. Right now the database write and the Kafka publish are two
separate operations — if Kafka is unreachable the expense saves but the event is lost. That's a real
correctness gap and it's the first thing I'd fix.

Second, alert suppression. Currently every expense past 80% raises a new alert, so a user who logs
five things in a row gets spammed. I'd add deduplication on user plus category plus month.

Third, integration tests with Testcontainers. All my tests use mocks, which means they verify my
logic against my assumptions about Postgres and Kafka. A wrong SQL query would pass every test I
have.

Beyond those — refresh tokens, rate limiting on the login endpoint, and correlation IDs propagated
through the Kafka headers so I can trace one request across all three programs."

---

## SECTION B — KAFKA AND EVENT-DRIVEN (Q7–Q20)

### Q7. What is Kafka, in your own words?

"It's a durable, ordered log that multiple programs can read independently.

The key thing that makes it different from a normal message queue: when a consumer reads a message,
it isn't deleted. It stays on disk. Each consumer group keeps its own bookmark — its offset — of how
far it's read.

That's why I can have two completely different consumers reading the same stream, and why I could add
a third next month that replays the last week of history to catch up."

### Q8. Why Kafka and not RabbitMQ?

"The one-line difference is that RabbitMQ deletes a message once it's acknowledged, and Kafka keeps
it on disk with each reader tracking its own position.

That gave me four things I needed.

Multiple independent readers — my budget evaluator and my notification dispatcher are separate
consumer groups reading the same topic, and both see every message. In RabbitMQ I'd need fanout
exchanges set up in advance.

Replay — if I add an analytics service, it can start from the beginning and backfill history. With
RabbitMQ those messages are gone. It's also my bug recovery plan: if my threshold logic was wrong, I
fix it, reset the offset, and reprocess.

Ordering with parallelism — keying by user id means one user's events stay in order, but different
users process in parallel. RabbitMQ only guarantees ordering with a single consumer, so you have to
choose between order and throughput.

And throughput — Kafka appends to a log file, which is the fastest thing a disk does.

That said, RabbitMQ is much simpler to operate, and it has things Kafka doesn't — priority queues,
delayed delivery, per-message TTL. If I just needed a task queue with no replay, RabbitMQ would be
less machinery for the same result."

### Q9. Couldn't you have just used @Async or a thread pool?

"That's exactly what I tried first, actually.

My first version called the budget logic directly inside the POST handler and the endpoint was
sitting around 480 milliseconds in Postman. So my first fix was `@Async`. Response time dropped
immediately and I thought I was done.

Then I killed the app mid-request while testing something unrelated, and the alert just never
happened. The expense was in the database, the notification wasn't, and there was no record anywhere
that it should have been.

That's when it clicked. `@Async` hands work to an in-memory thread pool. If the JVM dies, the queue
dies with it. No durability, no retry, no visibility into what was lost, and you can't scale the
background work separately from the API.

Kafka gave me three things a thread pool structurally cannot: the event is on disk before my API
returns, consumers can be scaled and deployed independently, and because Kafka retains messages I
could add a second consumer group later without touching the producer. I actually did that — I split
one consumer into two, and `ExpenseService` didn't change by a single line."

### Q10. Explain topics, partitions, and consumer groups.

"A **topic** is a named stream of messages. I have two — `expense-events` and `notification-events`.

Each topic is split into **partitions**, which are like lanes. I use three. Each partition keeps its
messages strictly in order, and each one can be read by a different thread — so partitions are the
unit of parallelism. One partition would cap me at one consumer thread no matter how many servers I
deploy.

A **consumer group** is a team of consumers sharing work. Within one group, each partition is assigned
to exactly one member — so members split the work rather than duplicating it. That's how you scale
one service horizontally.

But different groups each see **all** messages independently. That's how I have two different
services reading the same stream. My budget evaluator is in group `budget-evaluator`, my notifier is
in `notification-dispatcher`."

### Q11. Two consumers read the same topic. Why don't they steal each other's messages?

"Because they're in different consumer groups — and I learned that the hard way.

When I first split my single consumer into two, I left them both in the default group. Each event
only got handled by one of them, seemingly at random. Alerts were disappearing about half the time
and I couldn't work out why.

The reason is that a consumer group is Kafka's unit of work distribution. Within one group, each
partition goes to exactly one member, so they share the work. That's what you want when you're
scaling one logical service across three instances — you don't want all three doing the same job.

But I wanted two *different* services each seeing every message. So I gave them distinct group IDs.
Each group keeps its own offset into the same topic, so both see every event. I can restart one
without affecting the other, and scale them separately based on their own load."

### Q12. Why did you key messages by userId?

"I didn't, initially. I let the producer round-robin with a null key, because with three partitions
that spreads load evenly, which seemed obviously correct.

The problem showed up when I was testing rapid edits. I created an expense and immediately updated
the amount, and occasionally the budget alert reflected the *old* amount. The two events had landed
on different partitions and were being consumed concurrently by different threads, so the update
sometimes got processed before the create.

The rule I'd missed is that Kafka only guarantees ordering **within a partition**, never across a
topic. Keying by userId means Kafka hashes the key, so all of one user's events go to one partition
and stay in order. I still get parallelism — it's just across users instead of within one, which is
the correct axis anyway.

The trade-off is hot partitions. If one key generated a huge share of events, that partition becomes
a bottleneck while the others sit idle. For a personal finance app that's not realistic. On a
multi-tenant B2B system where one enterprise customer might be 40% of traffic, you'd key by
something with better distribution."

### Q13. Kafka is at-least-once. How do you avoid duplicate notifications?

"I hit this for real and it was slightly embarrassing.

I was restarting the app constantly while developing, and I noticed my notifications table had three
identical 'you exceeded your food budget' rows with different IDs. My first instinct was that I had
a bug in the threshold logic firing multiple times.

It wasn't. My consumer was saving the row, and then I was killing the app before the offset got
committed. On restart, Kafka redelivered from the last committed offset — which is correct behaviour,
because that's exactly what at-least-once means. My consumer just wasn't idempotent.

The fix was to make every event carry a UUID `eventId` generated by the producer, store it on the
notification row, and put a UNIQUE constraint on that column. The consumer calls `existsByEventId`
before inserting.

I deliberately did both — the application check and the database constraint. The app check gives a
clean skip-and-log. But two consumer threads could pass the `exists` check at the same moment, and
only the constraint can actually stop that. So the check is an optimisation; the constraint is the
correctness.

The general lesson is that you don't get exactly-once for free. You get at-least-once from the
broker, and you build exactly-once *effects* by making consumers idempotent."

### Q14. What happens if a consumer throws an exception?

"I found this the hard way too.

I had a null-handling bug in my message formatter and one bad event caused the consumer to throw.
Because the offset never committed, Kafka redelivered the same record — which threw again. My console
was scrolling infinitely.

But the spam wasn't the real problem. The real problem was that **every other message in that
partition was stuck behind it.** One malformed event had halted the pipeline for every user whose ID
hashed to that partition. That was genuinely alarming to watch.

So I configured a `DefaultErrorHandler` with a one-second backoff and two retries, plus a
`DeadLetterPublishingRecoverer`. Now a transient failure — say Postgres is briefly unreachable —
usually succeeds on the second attempt. A genuinely poisoned message gets three attempts, then goes
to `expense-events.DLT` with the exception in the record headers, the offset commits, and the
partition keeps moving.

I also wrapped the deserializer in `ErrorHandlingDeserializer`, which was a separate incident. I
changed a field type on an event while old messages were still in the topic, and the
deserialisation failure happened *before* my error handler could even see it — it crashed the
listener container on startup. `ErrorHandlingDeserializer` turns that into a handled failure the
error handler can route to the DLT.

The honest gap right now is that nothing alerts me when a message lands in the DLT. In production
I'd monitor DLT lag."

### Q15. What is a dead letter topic and why do you need one?

"It's a quarantine bin. When a message fails every retry, instead of blocking the partition forever
or silently dropping it, Kafka writes it to a separate topic — mine is `expense-events.DLT` — along
with the exception details in the headers.

Two reasons it matters. First, the pipeline keeps moving — one bad message can't freeze everything
behind it. Second, nothing is silently lost. I can go and look at what failed and why, fix the bug,
and replay those messages if needed.

Without a DLT your only two options are both bad: retry forever and block the queue, or drop the
message and never know it happened."

### Q16. What's `acks=all` and why did you use it?

"It controls when Kafka tells my producer 'got it'.

`acks=0` means don't wait at all — fastest, but you can lose messages silently. `acks=1` means wait
for the leader broker only — but if that leader dies before replicating, the message is gone.
`acks=all` means wait until every in-sync replica has it.

I chose `acks=all` because losing a budget alert because one broker restarted isn't acceptable for
this use case. The latency cost is a few milliseconds, and it doesn't affect my API response time at
all because the send is asynchronous anyway — the user isn't waiting for the acknowledgement."

### Q17. What's the idempotent producer setting for?

"`enable.idempotence=true`. It handles a specific edge case on the producer side.

If my producer sends a message and the broker receives it but the acknowledgement gets lost on the
way back, the producer will retry — and now the same message is in the topic twice. With idempotence
enabled, Kafka assigns each producer a sequence number and deduplicates automatically.

Worth being precise here: this only protects against duplicates *caused by producer retries*. It
doesn't give you end-to-end exactly-once. That's why I still needed the `eventId` check on the
consumer side."

### Q18. Why two topics instead of one?

"Because they represent two different kinds of facts, and they have different audiences.

`expense-events` says 'a user spent money'. That's raw domain data. Analytics would want it, a
categorisation model would want it, an audit log would want it.

`notification-events` says 'someone needs to be told something'. That's a much narrower, already-
processed signal.

Keeping them separate means a future notification channel — SMS, push — subscribes to
`notification-events` and doesn't have to know anything about expenses or budget rules. And the
budget logic lives in exactly one place instead of being duplicated in every notification channel.

It also gives me two independent retry and failure domains. If the notification consumer is broken,
the budget evaluation still works fine."

### Q19. What's the difference between an event and a command?

"A command says *do this* — 'SendEmail'. An event says *this happened* — 'ExpenseCreated'.

It sounds like naming pedantry but it changes the coupling completely. If my producer published
'SendEmail', it would have to know that emails are a thing that happens, and I'd have to change it
when I add SMS.

Because it publishes 'ExpenseCreated' — a fact about the past — it doesn't know or care who's
listening. Adding a new consumer requires zero changes to the producer. That's what actually makes
the system extensible, and it's the difference between using Kafka and using it well."

### Q20. How would you scale this if traffic went up 100x?

"Different answers for the two halves.

For the API, it's stateless because of JWT, so I just run more instances behind a load balancer. No
session affinity needed. The database connection pool would need tuning and Postgres would likely
need read replicas for the summary queries.

For the consumers, I'd add partitions and run more consumer instances in the same group — Kafka
automatically rebalances partitions across group members. The ceiling is the partition count: with
three partitions I can't usefully run more than three consumer threads, so I'd raise that first.
Worth noting you can increase partitions but never decrease them, so you plan ahead.

The thing I'd watch is the SUM query in the budget evaluator. At 100x it runs constantly. I'd
probably maintain a running monthly total in a separate table updated incrementally, rather than
recalculating from scratch on every event."

---

## SECTION C — DATABASE AND PERFORMANCE (Q21–Q32)

### Q21. You claim sub-100ms. Prove it. What was slow and how did you find it?

"The list endpoint was the problem, and I only found it because I forced myself to seed realistic
data. With 30 rows everything is fast and you learn nothing, so I generated 200,000 expenses with a
`generate_series` insert. `GET /expenses` with a date filter went to roughly 380 milliseconds.

I turned on Hibernate SQL logging, copied the generated query into psql, and ran `EXPLAIN ANALYZE`.
Postgres was doing a Sequential Scan across all 200,000 rows, and then a *separate* Sort node to
order by date. The sort alone was a big chunk of the time.

I added a composite index on `(user_id, spent_at DESC)`. Two details mattered, and I got the first
one wrong initially. **Column order** — `user_id` has to come first because it's the equality
condition. I first tried `(spent_at, user_id)` and it barely helped, because Postgres can't
efficiently seek on the second column without a constraint on the first. **Direction** — I put DESC
in the index and made sure my JPA Sort matched exactly, which made the Sort node disappear entirely.
Postgres just walks the index backwards.

The plan went from Seq Scan plus Sort to a clean Index Scan, and the endpoint landed around 15
milliseconds.

Separately, the summary endpoint was originally fetching every expense for the month and summing in
Java. I moved it to a GROUP BY with an interface projection, so the database returns nine rows
instead of two thousand objects for Hibernate to build.

I also enabled percentile histograms in Actuator, because I got burned early trusting an average —
the mean looked fine while the p99 was terrible."

### Q22. Explain what an index actually does.

"It's the index at the back of a textbook. Without it, to find every mention of 'photosynthesis' you
read all 600 pages. With it, you look up the word and jump straight to pages 47 and 203.

In a database it's usually a B-tree — a sorted structure that lets you find a value in roughly log(n)
steps instead of scanning n rows. For 200,000 rows that's about 18 steps versus 200,000.

The cost is that every INSERT, UPDATE and DELETE has to maintain the index too, so writes get
slightly slower and it uses disk space. Which is why you index the columns you actually filter and
sort on, not every column."

### Q23. Why is column order in a composite index important?

"Because a composite index is sorted by the first column, then the second within that, and so on.
Like a phone book sorted by surname then first name.

If you know the surname, you can jump straight there and then scan within it. If you only know the
first name, the phone book is useless — you're back to reading every page.

My index is `(user_id, spent_at DESC)`. Every query filters by user_id with equality, so that must
come first. Then within one user's rows, they're already sorted by date, which is exactly what my
query wants.

I actually tried `(spent_at, user_id)` first and it barely helped, which is how I learned this
properly rather than from a blog post."

### Q24. Why PostgreSQL and not MongoDB?

"Four reasons, strongest first.

My data is genuinely relational — an expense belongs to a user, is checked against a budget, rolls
up into a summary. That's joins and aggregations.

ACID transactions matter for money. A half-written financial record is corruption. Postgres
guarantees all-or-nothing by default.

Postgres has `NUMERIC(12,2)` which stores decimals exactly. MongoDB stores numbers as floats by
default, and storing money as a float is a bug with a delayed fuse — you don't notice until your
totals are off by three paise across ten thousand rows.

And my constraints live in the database — `CHECK (amount > 0)`, `UNIQUE (user_id, category)`. Those
are enforced by the engine itself, so even a buggy client can't violate them. In MongoDB those rules
live in application code and every program that touches the data has to remember them independently.

The honest counter is that if my schema were genuinely unpredictable per user, or I needed to shard
across regions from day one, MongoDB's flexibility and built-in sharding would be a much easier path."

### Q25. What's the N+1 problem and how did you avoid it?

"It's when you run one query to fetch a list, then accidentally run one more query per item in that
list. Fetch 20 expenses, then fetch each one's user separately — that's 21 queries instead of 1.

It usually happens silently because ORMs make lazy loading invisible. You write `expense.getUser()
.getName()` in a loop and it looks like normal Java.

I avoided it three ways. My `@ManyToOne` relationships are all `LAZY`, so nothing loads unless I ask.
I set `open-in-view: false`, which stops lazy loading happening during JSON serialisation — that's a
really common hidden cause. And my DTOs only contain the fields I actually return, so I never touch
the user object at all.

If I did need related data, I'd use a JOIN FETCH query or an entity graph to load it in one round
trip."

### Q26. Why BigDecimal and not double for money?

"Because binary floating point literally cannot represent most decimal fractions exactly.

`0.1 + 0.2` in a double gives `0.30000000000000004`. That's not a bug in Java, it's how IEEE-754
works — 0.1 in binary is a repeating fraction, like 1/3 in decimal.

For one calculation it doesn't matter. Across a million transactions, those errors accumulate and
your books don't balance. And you can't ever explain to a user why their total is off by two paise.

`BigDecimal` stores the digits and the scale separately, so it's exact. It's slower and more verbose
— you have to use `.add()` instead of `+` and `.compareTo()` instead of `==` — but for money that's
non-negotiable. My database column is `NUMERIC(12,2)` to match."

### Q27. What is a transaction and where did you use one?

"A transaction is a group of database operations that either all succeed or all fail. No half-done
states.

In my code it's the `@Transactional` annotation. `ExpenseService.create` is transactional — if
anything throws after the insert, the insert is rolled back and it's as if nothing happened.

The ACID properties: Atomicity is the all-or-nothing part. Consistency means your constraints always
hold. Isolation means concurrent transactions don't corrupt each other. Durability means once it's
committed, it survives a crash.

One thing I'd flag — my Kafka publish is *inside* the transaction but Kafka isn't part of it. That's
the dual write problem, and I know about it."

### Q28. Why Flyway instead of hibernate ddl-auto?

"`ddl-auto=update` looks at your Java classes and guesses what to change in the database. It never
drops or renames anything, so your local schema and production schema silently drift apart, and
there's no record of what changed or why.

Flyway makes migrations versioned SQL files in Git. They're reviewable in a pull request, they run in
a fixed order, and they're identical on every machine.

And I run Hibernate in `validate` mode — so if my entities and the schema disagree, the app refuses
to start. Loud failure at boot beats a silent failure at 2am on one specific query path.

One rule I had to learn: never edit a migration after it's run. Flyway checksums them and will
refuse to start. New changes go in a new V2 file."

### Q29. How does your summary endpoint work and why is it fast?

"It's one query. `SELECT category, SUM(amount), COUNT(*) FROM expenses WHERE user_id = ? AND
spent_at BETWEEN ? AND ? GROUP BY category`.

Two things make it fast. The aggregation happens **inside Postgres** — I'm not pulling 2,000 rows
into Java and summing them in a loop. And I use a Spring Data interface projection, `CategoryTotal`,
so Hibernate builds nine tiny objects with three fields each instead of hydrating 2,000 full entity
objects with all their relationships.

My first version did the naive thing — fetch everything, sum in Java. It worked fine with test data
and fell over completely at 200,000 rows."

### Q30. What is a connection pool?

"Opening a database connection involves a TCP handshake and authentication — roughly 30 milliseconds.
If you did that per request, that's 30ms of pure overhead on every single call.

A connection pool keeps a set of connections open and hands them out. My config uses HikariCP, which
Spring Boot includes by default, with a maximum of 10 connections.

Why 10 and not 100 — more connections isn't automatically better. Each one costs memory on the
Postgres side, and past a point you're just creating contention. The rough rule is
`(core_count × 2) + effective_spindle_count`. For a local setup, 10 is plenty."

### Q31. How do you handle pagination?

"Standard page and size query parameters, defaulting to page 0 and size 20.

Two things I did deliberately. I **cap the page size at 100** — otherwise someone requests
`size=1000000` and you've handed them a denial-of-service tool against your own database. There's a
test that asserts this.

And I sort by `spentAt DESC, id DESC` — the same order as my index, so Postgres never does a separate
sort step. The secondary sort on id is for stable ordering when two expenses have the same date.

I also wrap it in my own `PageResponse` record rather than returning Spring's `Page` object directly,
because Spring's serialises with a lot of internal noise and its JSON shape isn't stable across
versions.

If this needed to scale much further I'd switch to keyset pagination — `WHERE spent_at < last_seen`
— because OFFSET gets slower the deeper you page."

### Q32. What database constraints did you add and why?

"Several, and I put them in the database deliberately rather than only in application code.

`CHECK (amount > 0)` — an expense can't be negative or zero.
`UNIQUE (email)` on users — no duplicate accounts.
`UNIQUE (user_id, category)` on budgets — one budget per category per user.
`UNIQUE (event_id)` on notifications — this is what makes my consumer idempotency bulletproof.
Foreign keys with `ON DELETE CASCADE` — deleting a user cleans up their data automatically.

The reasoning is that application validation can be bypassed. A future service, a migration script,
someone in psql — none of them run my Java code. The database constraint is the last line of
defence and it can't be skipped.

I still validate in the application too, because that gives a nice 400 with a helpful message instead
of a raw constraint violation. But the database is the actual guarantee."

---

## SECTION D — SECURITY AND JWT (Q33–Q41)

### Q33. Explain JWT and how you used it.

"A JWT is a token with three parts separated by dots — a header saying which algorithm, a payload
with the data, and a signature.

The important property is that it's **self-verifying**. The signature is computed from the header and
payload using a secret only my server knows. If anyone changes a single byte of the payload, the
signature no longer matches and verification fails. So the token itself proves it's genuine — I don't
need to look anything up.

In my flow: user logs in, I check their password with BCrypt, and issue a token containing their
email as the subject, their numeric user ID as a custom claim, issued-at, and expiry. They send it
on every request as `Authorization: Bearer <token>`.

My filter verifies the signature and expiry, pulls the user ID out, and puts it in Spring's security
context. **Zero database queries.**"

### Q34. Why JWT instead of sessions?

"Sessions store state on the server. If I run three instances behind a load balancer, either every
request looks up the session in Redis — a network round trip on my critical path — or I use sticky
sessions and lose clean failover when an instance dies.

I actually hit this. I ran two instances locally on different ports to simulate a load balancer, and
logging in on one meant getting 401s from the other. Sessions live in that instance's memory.

A JWT carries its own proof, so any instance can verify it with the shared secret and zero network
calls. That's what makes the API horizontally scalable."

### Q35. What's the weakness of JWT and how do you handle it?

"I can't revoke it. That's the fundamental trade-off and I'd rather state it than have it found.

If a user logs out, or their token is stolen, it stays valid until it expires. There's no server-side
list to remove it from — that's the whole point of being stateless.

I mitigate it with a 60-minute expiry, which bounds the damage window. But that's mitigation, not a
solution.

The proper design is short-lived access tokens — 15 minutes — plus long-lived refresh tokens stored
server-side that can be revoked. You get most of the statelessness benefit on the hot path while
keeping the ability to log someone out. That's the next thing I'd build.

The other thing worth saying: never put sensitive data in a JWT payload. It's base64-encoded, not
encrypted — anyone can decode and read it. The signature stops tampering, not reading. Mine only
contains an email and a numeric ID."

### Q36. What optimisation did you make in your JWT implementation?

"Most JWT tutorials do this: verify the token, extract the username, then call `UserDetailsService`
to load the user from the database — on every single request.

That defeats half the purpose. You've kept the statelessness but put a database query right back on
your hot path.

I put the numeric user ID in the token as a custom `uid` claim, and built my own `AppUserPrincipal`
that carries it. So my filter reconstructs the principal directly from the token. **Authenticating a
request costs zero database queries.**

Spring's built-in principal only holds the username, which is why I needed a custom one. My
`CustomUserDetailsService` still exists, but it's only used during login."

### Q37. Why BCrypt and not SHA-256?

"Because SHA-256 is designed to be fast, and that's exactly wrong for passwords.

A normal gaming GPU can compute billions of SHA-256 hashes per second. So if someone steals your
database, they brute-force every common password in hours.

BCrypt is deliberately slow — 2^10 rounds by default, which is about 100 milliseconds per hash. That
100ms is invisible to a user logging in once, but it makes brute-forcing millions of guesses
completely impractical.

It also salts automatically. Every password gets random data mixed in before hashing, so two users
with the same password get completely different hashes. That kills rainbow tables — precomputed
lookup tables of common password hashes.

My `AuthServiceTest` actually asserts the stored value starts with `$2a$`, which is BCrypt's marker,
so I can prove hashing really happened.

The honest note: Argon2id is the current recommendation for brand new systems because it's
memory-hard as well as slow. BCrypt is the ubiquitous, well-audited default."

### Q38. How do you stop one user reading another user's expenses?

"Two layers.

First and most important: **the user ID comes from the signed token, never from the request.** My
controllers use `@AuthenticationPrincipal` to get the ID that the JWT filter extracted. If it came
from a query parameter like `?userId=7`, anyone could change a number and read anyone's data. That
single design choice is the whole ownership model.

Second, my repository methods are scoped. `findByIdAndUserId(id, userId)` does the ownership check
**inside the SQL query**, not in Java afterwards. That's faster, and it also means I return 404
rather than 403 for someone else's expense — which matters, because a 403 tells an attacker that ID
exists."

### Q39. Why did you disable CSRF?

"CSRF attacks work by exploiting the browser's automatic sending of cookies. A malicious site makes
your browser send a request to my API, and the browser helpfully attaches your session cookie.

My API doesn't use cookies at all. The JWT is sent in an `Authorization` header, which browsers never
attach automatically — JavaScript on my frontend has to explicitly add it. A malicious site can't
make that happen.

So there's no attack surface for CSRF to protect against, and leaving it enabled would just break
legitimate requests.

I want to be precise though — this is only safe *because* I'm not using cookies. If I stored the JWT
in a cookie, CSRF would absolutely be a risk again and I'd need protection."

### Q40. Someone sends an expired or tampered token. Walk me through what happens.

"My filter calls `jwtService.parseAndValidate()`, which does both checks in one operation — it
verifies the HMAC signature and checks the expiry claim.

For a tampered token, the recomputed signature won't match the one in the token, and jjwt throws a
`SignatureException`. For an expired one it throws `ExpiredJwtException`. Both extend `JwtException`,
which I catch.

When I catch it, I clear the security context, log at debug level, and let the request continue —
**unauthenticated**. I deliberately don't throw from the filter.

Then the authorisation rules take over. Since my config is `.anyRequest().authenticated()` and there's
no authentication, Spring calls my `AuthenticationEntryPoint`, which returns a 401 with my standard
JSON error body.

My `JwtServiceTest` covers all three negative cases explicitly — wrong secret, expired, tampered
payload. I deliberately wrote more tests for the failure cases than the happy path, because in
security that's where the bugs matter."

### Q41. What other security measures did you take?

"A few beyond the obvious.

**Deny by default.** My config is `.anyRequest().authenticated()` with an explicit allowlist. So any
endpoint I add in future is automatically protected. The opposite pattern — allow everything, list
the protected ones — leaks a new endpoint the day someone forgets to update the list.

**Vague login errors.** A failed login always returns 'Invalid email or password', never 'no user
with that email'. Otherwise I've built a tool for attackers to discover which emails are registered.
That's called user enumeration.

**No stack traces to clients.** My `GlobalExceptionHandler` logs the full trace server-side and
returns a generic message. Stack traces leak your framework versions and internal class structure.

**Secrets from environment variables.** The JWT secret is `${JWT_SECRET}` in config, never hardcoded.
`.env` is gitignored.

**Validation at the boundary.** Bean Validation on every DTO, so malformed input dies at the
controller and never reaches business logic.

**Password never leaves the server.** My DTOs are separate from entities, so the hash physically
cannot be serialised into a response by accident.

What's missing, honestly: rate limiting on the auth endpoints. They're my only publicly reachable
surface and therefore the brute-force target. That's a real gap."

---

## SECTION E — SPRING AND JAVA (Q42–Q48)

### Q42. What is dependency injection and why does it matter?

"Instead of a class creating the things it needs, they're handed to it from outside.

So `ExpenseService` doesn't do `new ExpenseRepository()`. It takes the repository in its constructor,
and Spring provides it at startup.

Two reasons it matters. First, **testability** — in my unit tests I hand it a Mockito mock instead of
a real repository, so I can test the business logic with no database running. That's only possible
because the class doesn't create its own dependencies. Second, **swappability** — I could replace the
implementation without touching the class that uses it.

I use constructor injection rather than `@Autowired` on fields, because it makes dependencies
explicit, allows the fields to be final, and means the object can never exist in a half-constructed
state."

### Q43. Explain the layers: Controller, Service, Repository.

"Each has one job and that separation is deliberate.

**Controller** handles HTTP only. Read the request, call the service, return a status code. No
business logic at all. If I added a GraphQL API tomorrow, I'd write a new controller and reuse
everything below.

**Service** is where the actual logic lives. Business rules, transaction boundaries, publishing
events. This is the layer that would survive any change of framework.

**Repository** talks to the database. With Spring Data I only declare an interface — I write no
implementation. Spring generates it at startup from the method name. `findByIdAndUserId` automatically
becomes the right query.

The test of whether you've done this right: could you swap Postgres for MySQL by only changing the
repository layer? For most of my code, yes."

### Q44. What are DTOs and why not just return entities?

"A DTO is a separate class used only for API input and output.

Four reasons I don't return entities directly.

**Security** — my `User` entity contains the password hash. If I returned it, that hash is in the JSON.
A DTO makes that physically impossible rather than relying on remembering an annotation.

**Decoupling** — if I rename a database column, my API contract doesn't break. Clients don't have to
change because I refactored internals.

**Validation** — my request DTOs carry the validation annotations, so bad input is rejected at the
controller boundary before it touches anything.

**Avoiding lazy loading traps** — serialising an entity with lazy relationships can trigger extra
queries or throw `LazyInitializationException` during JSON writing. A DTO has plain values.

I use Java records for them, which gives immutability with almost no boilerplate."

### Q45. What is @Transactional actually doing?

"Spring wraps the method in a proxy. When you call it, the proxy opens a database transaction,
runs your method, and then either commits — if it returned normally — or rolls back if it threw a
runtime exception.

Three gotchas I know about.

**It only works through the proxy.** If a method inside the same class calls another `@Transactional`
method directly, the annotation is ignored, because the call doesn't go through the proxy.

**By default it only rolls back on unchecked exceptions.** A checked exception commits unless you
specify `rollbackFor`.

**Read-only transactions are a real optimisation.** I use `@Transactional(readOnly = true)` on my
query methods, which lets Hibernate skip dirty-checking and can let the database route to a replica."

### Q46. How do you handle errors and exceptions?

"One central `@RestControllerAdvice` class called `GlobalExceptionHandler`.

It maps each exception type to a status code and a consistent `ApiError` body — validation failures
to 400 with a list of exactly which fields failed, my `ResourceNotFoundException` to 404, duplicate
email to 409, bad credentials to 401, and a catch-all for anything unexpected to 500.

Three things I made sure of. **Consistency** — every error has the same JSON shape, so a client never
has to guess. **No leakage** — the catch-all logs the full stack trace server-side but returns a
generic message. **Useful validation errors** — the 400 includes a `details` array naming each field
and what was wrong with it, so a frontend can highlight the right input.

Without this, a validation failure returns Spring's default error page and an unexpected exception
leaks your internals to whoever's calling."

### Q47. Why Java records for your DTOs and events?

"Records give you immutability, a constructor, getters, `equals`, `hashCode` and `toString` in one
line.

For DTOs it means less boilerplate and no accidental mutation. For **events** immutability is
genuinely important — an event is a fact about something that already happened. If a consumer could
modify the event object mid-processing, you'd have an extremely confusing class of bug.

The alternative is Lombok, which I deliberately didn't use. Records are a language feature, so they
work everywhere with no annotation processor and no IDE plugin needed. For entities I wrote plain
classes with getters and setters, because JPA needs a no-arg constructor and mutable fields."

### Q48. What does Spring Boot actually do for you?

"Three big things.

**Auto-configuration.** It looks at what's on your classpath and configures sensible defaults.
Postgres driver present? It configures a DataSource. Spring Web present? It starts an embedded Tomcat
on port 8080. Without it you'd write hundreds of lines of XML or config classes.

**Starters.** `spring-boot-starter-web` is one dependency that pulls in Tomcat, Spring MVC and Jackson
at versions guaranteed to work together. It removes an entire category of dependency hell.

**Embedded server.** The app is a runnable JAR with Tomcat inside it. No deploying a WAR to an
external server — you just run it, which is exactly what you want for containers.

The key thing to understand is that auto-configuration is **conditional and overridable**. The moment
I define my own `SecurityFilterChain` bean, Spring backs off and uses mine. That's how I customised
security and Kafka while keeping the defaults everywhere else."

---

## SECTION F — TESTING (Q49–Q51)

### Q49. What did you test and why those things?

"14 tests across four classes, and I chose them by risk rather than chasing a coverage number.

`BudgetEvaluationConsumerTest` has the most cases because the threshold logic is the actual business
rule of the project. I test all four branches — no budget set, under 80%, exactly at 85% giving a
WARNING, and 120% giving EXCEEDED.

`JwtServiceTest` is deliberately weighted toward **failure** cases — wrong secret, expired token,
tampered payload. In security, 'it works' isn't the interesting question. 'Does it correctly reject
bad input' is.

`ExpenseServiceTest` covers that creating publishes exactly one event, that another user's expense
returns 404, and that page size is capped at 100.

`AuthServiceTest` proves the password is actually hashed — it asserts the stored value starts with
`$2a$` — and that a duplicate email returns 409 without hitting the database save."

### Q50. How do you test a Kafka consumer without Kafka?

"The insight is that a `@KafkaListener` method is just a normal Java method. Nothing about the
annotation changes how it behaves when you call it directly.

So in my test I construct an `ExpenseCreatedEvent` and call `consumer.onExpenseCreated(event)`
directly, with mocked repositories and a mocked publisher. Then I use an `ArgumentCaptor` to grab
the alert event that was published and assert its severity and percentage.

The whole class runs in about 200 milliseconds with no broker and no database. That speed is what
makes it realistic to run on every commit.

What this **doesn't** test is serialisation, the actual Kafka wiring, or offset handling. For that
you'd need Testcontainers or an embedded broker — that's a gap I'm aware of."

### Q51. What are mocks and what's the downside?

"A mock is a fake version of a dependency where you control what it returns. `when(repo.findById(1))
.thenReturn(Optional.empty())` means 'pretend this ID doesn't exist'.

It lets me test my logic in isolation — no database, no network, milliseconds instead of seconds.

The downside is real and I'd rather name it: **mocks verify my code against my assumptions.** I've
told the mock what the repository returns. If my actual JPQL query is wrong, or if Postgres behaves
differently than I assumed, my tests still pass happily.

So mocks prove my logic is right *given* correct dependencies. They don't prove the dependencies are
correct. That's what integration tests are for, and using Testcontainers to spin up a real Postgres
and real Kafka is the honest next step."

---

## SECTION G — DESIGN AND TRADE-OFFS (Q52)

### Q52. Your expense saves to Postgres, then publishes to Kafka. What if the publish fails?

**This is the sharpest question on the list. It has a name and they want to hear you say it.**

"That's the **dual write problem**, and my implementation has it. I know it's there.

The database commit and the Kafka publish are two separate systems with no shared transaction. If the
broker is unreachable at that exact moment, the expense is saved but the event never exists — and the
budget alert silently never fires. There's no exception the user sees, because I deliberately swallow
publish failures so that a Kafka outage can't fail a legitimate write.

I made that trade-off consciously. I'd rather lose an alert than reject a valid expense. But it's a
correctness gap, not a design feature.

The proper fix is the **transactional outbox pattern**. Instead of calling Kafka from the service, you
insert the event into an `outbox` table in the *same* transaction as the expense. That's atomic —
either both land or neither does. Then a separate relay process, either polling that table or reading
the Postgres write-ahead log with something like Debezium, publishes to Kafka and marks the row sent.
You accept a little extra latency and get an actual guarantee.

I'd also point out that the ordering I chose matters. Publishing *before* committing would be
strictly worse — you could emit an event for an expense that then rolls back, which means alerting a
user about something that never happened. Committing first means my failure mode is a missing alert,
not a phantom one. Given I had to pick one, that's the right direction."

---

# PART 9 — YOUR 2-MINUTE PITCH (Memorise This)

When they say "tell me about your project", this is what you say. Practise until it's smooth.

> "It's an expense tracking backend — a REST API in Java and Spring Boot. Users record what they
> spend, set monthly budget limits per category, and get alerted when they're about to go over.
>
> The interesting part is the architecture. When someone saves an expense, the API does exactly two
> things: it writes to Postgres and publishes an event to Kafka. Then it returns — under 100
> milliseconds.
>
> Everything else happens in the background. A Kafka consumer picks up the event, calculates their
> monthly spend in that category, compares it to their budget, and if they're past 80% it publishes
> a second event. A different consumer group picks that up and creates the notification.
>
> I built it this way because my first version did all of that synchronously and the endpoint was
> taking 480 milliseconds — most of it waiting on the notification step. Worse, if the notification
> logic failed, the user couldn't save their expense at all. Decoupling it through Kafka fixed both
> problems, and it means adding a new consumer later doesn't touch the write path at all.
>
> On the data side, I optimised the list query with a composite index on user and date, which took it
> from about 380 milliseconds to 15 on a 200,000-row table. Auth is stateless JWT with the user ID
> in the token, so authenticating a request costs zero database queries. And I've got 14 JUnit tests
> covering the budget threshold logic and the JWT failure cases.
>
> The thing I'd fix first is the dual write problem — the database commit and the Kafka publish
> aren't atomic, so a broker outage could lose an event. The proper solution is the transactional
> outbox pattern."

**Why this works:** it says what it does, why the architecture exists, gives concrete numbers, and
ends by volunteering a weakness. That last part is what makes you sound like an engineer rather than
someone who followed a tutorial.

---

# PART 10 — FINAL CHECKLIST

## Before the interview

- [ ] The project actually runs on your laptop. Test it the morning of.
- [ ] You've triggered the pipeline and seen the five log lines fire
- [ ] You've run `EXPLAIN ANALYZE` with and without the index and **written down your own numbers**
- [ ] You can draw the architecture diagram from memory in 60 seconds
- [ ] You can give the 2-minute pitch without looking at notes
- [ ] You can explain the restaurant analogy smoothly
- [ ] You've read through your own code once more, especially `ExpenseService`, `JwtAuthenticationFilter`, and both consumers

## Things worth doing to make your stories real

These take one evening and they convert memorised answers into lived experience:

1. **Build the synchronous version.** Put the budget logic directly in `ExpenseService.create`.
   Measure it. Then refactor to Kafka and measure again. Now you have two real numbers.
2. **Remove the idempotency check.** Kill the app mid-consume, restart, watch duplicate notifications
   appear. Put it back.
3. **Throw an exception in a consumer.** Watch the retries, then find the message in the DLT at
   localhost:8081.
4. **Set the Kafka key to null.** Create and rapidly update an expense. Watch the partition numbers
   differ in the logs.
5. **Put both consumers in the same group.** Watch events get split instead of both processing.
6. **Seed 200,000 rows and drop the index.** Watch the query plan change from Index Scan to Seq Scan.

## Red flags to avoid saying

| Don't say | Say instead |
|---|---|
| "Kafka is just faster" | "Kafka retains messages so multiple consumers can read independently, and I can replay" |
| "I used JWT because it's secure" | "JWT is stateless, so any instance can verify it without a lookup — the trade-off is I can't revoke it" |
| "I don't know" | "I haven't hit that, but my instinct would be X — how do you handle it?" |
| "It's perfect" | "The dual write is a real gap — the outbox pattern is the fix" |
| "I followed a tutorial" | "I built it synchronously first, measured it, then refactored" |

## If you don't know an answer

Say: *"I haven't run into that directly. My instinct would be [reason through it out loud]. How does
your team handle it?"*

That's a perfectly good answer. Reasoning out loud from what you do know is what they're actually
assessing. Bluffing is the only thing that will genuinely hurt you — because the follow-up question
will expose it, and then everything else you said becomes suspect.

## Final note on honesty

Everything in this guide describes code you actually have. Run it, break it, fix it — then the
stories in Part 8 stop being things you memorised and become things you did.

That matters practically, not just ethically. Interviewers probe. "What was the p99, not the
average?" "Which index did the planner actually pick?" "Show me what's in the DLT." A memorised
answer collapses at the second follow-up. A real one just keeps going — and you'll answer calmly
instead of scrambling, which is itself the signal they're reading.

**Good luck. You've got a genuinely solid project here. Go run it a few times and own it.**
