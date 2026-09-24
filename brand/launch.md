# Launch Plan

## 01 Launch strategy overview

HayPaComer debuts as a working, honest prototype built in the open: the repository goes public with the brand applied, and v1.0 arrives as the hard moment when the demo passes every acceptance criterion. The story is measurement: a fridge that weighs, warns, and decides - built by a student who wanted evidence instead of another recipe app.

**Launch narrative:** The fridge that measures. A normal fridge plus an affordable tray becomes a system that knows its grams, respects its owners, and keeps working offline.

**Launch goal:** public repository live with green CI and the brand applied; one announcement post; one demo video showing the four hero moments (door 40 s, milk 842 to 650 g, chicken 200 vs 80 g, AI outage fallback). Success is measured by repo activity and course/portfolio recognition, not by virality.

**Launch type:** soft launch (repository + build log), phased, with v1.0.0 as the hard release moment.

## 02 Pre-launch phase

**Asset preparation checklist:**

- [x] Brand system complete (`brand/`, tokens, logo).
- [x] README with tagline, measured promise, and docs map.
- [ ] Repository public with CI badge green.
- [ ] Screenshots or short clips of the live panel and the scale flow.
- [ ] Demo video (2 to 3 minutes) following the demo script (`docs/demo`).
- [ ] Release notes for v1.0.0.

**Rehearsal (solo rollout):** run `mvn -q verify` green, run the full demo script end to end, verify no secrets in the repository (`git grep` check), confirm the README reads well on mobile.

**Warming:** a short build-log post per phase (F1 to F7 highlights) keeps a small thread of progress before the release; no teasers, no countdowns.

## 03 Launch day plan

| Order | Action | Channel | Owner |
|---|---|---|---|
| 1 | Repo public, history pushed | GitHub | Juan |
| 2 | CI green badge verified | GitHub Actions | Juan |
| 3 | Announcement post | LinkedIn + X | Juan |
| 4 | Demo video link in README and post | YouTube + GitHub | Juan |
| 5 | Engage with every comment for 48 h | All | Juan |

## 04 Launch content

**Main announcement post (draft):**

> I built a fridge that weighs what is inside. Not a camera that guesses - a tray that measures.
>
> HayPaComer keeps my real stock in grams, tells me what expires first, and answers the only question that matters at 6:40 p.m.: what can I cook right now?
>
> Door open 40 seconds: it warns me. Milk from 842 g to 650 g: it records 192 g consumed, container weight included. Chicken 200 g needed against 80 g on the tray: it offers to reduce or weigh a substitute. When the AI API goes down, the rules keep cooking.
>
> Java 25, ESP32, SQLite, 8 modules, 23 GoF patterns with tests, and a brand built like a product. Public repo in the comments.
>
> Weigh it. Know it. Cook it.

**Founder post (draft):**

> For years my fridge had three bags of onions and no answer for dinner. I stopped waiting for a smarter appliance and built the smallest honest thing: a scale that knows what is on the tray. Today the project goes public.

**Launch email:** not planned for v1.0; the audience is reached through the portfolio, university channels, and social posts.

## 05 Post-launch - first 30 days

- **Week 1:** reply to every comment and share; pin the demo video; fix anything the launch exposes within 48 hours.
- **Week 2 to 3:** publish two build-log posts: "how the scale discounts stock" and "why rules, not AI, decide safety".
- **Week 4:** review metrics, gather feedback from classmates and teachers, and open the first `good first issue` list for the pilot path.

## 06 Launch metrics

| Metric | Goal | How to measure |
|---|---|---|
| Repository live with green CI | Week of release | GitHub |
| Demo video views | Any sustained interest | YouTube |
| Stars and forks | Organic, no target gaming | GitHub |
| Course evaluation | Best possible grade | Presentation |
| Pilot conversations | 2 or more after release | Direct outreach |

## 07 Notes

- No press kit, no embargoes, no paid promotion: this is an honest portfolio launch.
- Every claim in launch copy must map to a demo moment, a test, or a number that can be weighed.
- Brand assets and copy follow `brand/brand-book.md`; any new claim gets a proof point first.
