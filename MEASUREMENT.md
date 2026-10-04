# How well does it actually work

Anyone can write a scam detector and say it works. This is what happens when you measure one.

## The corpus

I measured against a public, peer-reviewed dataset of real Azerbaijani SMS:

> Shahbazov, V. (2026). *SMS dataset for multi-class classification of ham, spam, and smishing
> in Azerbaijani language.* Problems of Information Technology, 17(1), 32–39.
> doi:[10.25045/jpit.v17.i1.04](https://doi.org/10.25045/jpit.v17.i1.04) ·
> [dataset](https://github.com/vusalshahbaz/sms-classification-dataset-azerbaijan)

4,538 messages: 3,037 legitimate (`ham`), 889 `spam`, 612 `smishing`. I did not build this
corpus and I do not redistribute it — it carries no licence file, so `tools/fetch-corpus.ps1`
downloads it from its own source and `corpus/dataset.csv` stays untracked.

## The method

The corpus was split in half once, with a fixed seed. The two halves are checked in as index
lists (`corpus/split_dev.json`, `corpus/split_test.json`).

- **DEV half** — the only half I looked at while writing rules and choosing weights.
- **TEST half** — scored at the end. Numbers from this half are the ones that count.

Shipping the index lists is the point. Saying "I held out a test set" is an assertion;
shipping the exact split is what lets someone else check it.

Reproduce it:

```
tools/fetch-corpus.ps1          # or tools/fetch-corpus.sh
./gradlew :app:testFullDebugUnitTest --tests '*CorpusMeasurementTest*'
```

## The result

On the held-out half — 298 smishing messages and 1,494 legitimate ones:

| | first version | this version |
|---|---|---|
| smishing caught (recall) | **19.8%** | **90.3%** |
| of the alarms, correct (precision) | 67.8% | 90.3% |
| legitimate messages falsely flagged | 1.9% | 1.9% |
| F1 | 30.6 | 90.3 |

The false-alarm rate did not move. The catch rate went up four and a half times.

For reference, both halves together: 92.2% recall, 91.0% precision, 1.8% false alarms.
The DEV half scores 93.9% recall — the 3.6-point gap to TEST is the honest cost of having
tuned on DEV, and it is small enough that the rules are not merely memorising it.

### Compared with machine learning on the same split

The dataset's own paper benchmarks TF-IDF models. I trained two of them on the DEV half and
scored them on the same held-out TEST half:

| | recall | precision | F1 |
|---|---|---|---|
| TF-IDF + LinearSVC | 84.6% | 76.8% | 80.5 |
| TF-IDF + Logistic Regression | 81.9% | 78.0% | 79.9 |
| **this detector (rules, on-device)** | **90.3%** | **90.3%** | **90.3** |

This is not a claim that rules beat machine learning in general. It is a claim about this
problem: scam SMS is written by people following a small number of scripts, and the scripts
have hard tells. A rule engine that encodes those tells needs no model file, no network call
and no training data at runtime, runs in under a millisecond on an old phone, and can say
*why* it fired. A classifier returns a number nobody can argue with.

## What the signals are worth

Measured on the DEV half: how often each signal fires on a smishing message, and how often it
fires on a legitimate one.

| signal | fires on smishing | fires on legitimate |
|---|---|---|
| `CALLBACK` — no link, but ring this number | 66.6% | 0.1% |
| `URGENCY` — blocked account, deadline, fine | 44.9% | 3.8% |
| `PRIZE_CLAIM` — you have won | 37.3% | 0.5% |
| `MONEY_PRIZE` — a specific sum as a prize | 31.8% | 0.1% |
| `PRIZE_LURE` — free, gift, coupon | 24.8% | 0.9% |
| `UNKNOWN_LINK_ACTION` — act on an unknown site | 16.2% | 0.3% |
| `INSECURE_HTTP` | 12.7% | 0.2% |
| `PREMIUM_RATE` — a per-minute price is printed | 8.6% | 0.0% |
| `BODY_BRAND_MISMATCH` — speaks for a bank, links elsewhere | 5.7% | 0.1% |
| `BRAND_MISMATCH` — bank's name inside a foreign domain | 2.5% | 0.0% |
| `SHORTENER` | 2.2% | 0.9% |
| `PREMIUM_SHORTCODE` — text a word to a short code | 2.2% | 0.3% |
| `SENDER_MISMATCH` | 1.3% | 0.0% |
| `IP_URL` | 0.6% | 0.0% |
| `SUSPICIOUS_TLD` | 0.3% | 0.0% |

## Three things measuring taught me that I had wrong

**1. Link shorteners are not a scam signal here.** Every guide lists `bit.ly` as a phishing
indicator. In this corpus shorteners appear in 36 legitimate messages and 9 smishing ones:
Azercell, Bakcell and mygov send real marketing SMS through `bit.ly`. The first version scored
a shortener at +3, which in Azerbaijan is close to a pure false-alarm generator. It now scores
+1 and cannot raise an alarm on its own.

**2. Four out of five missed scams had no link at all.** The first version only understood
links and urgency words, so it was blind to the largest family in the corpus: *ring this
number*, *text this word to this short code*, *you have won, call to claim*. Adding callback
detection moved recall more than every link rule combined.

**3. The attackers' spelling defeated the keyword list.** They write *təsdiqləyin* as
*tasdiqlayin*. The normaliser mapped `ə` to `e`, so the stored keyword was `tesdiq` and the
incoming word was `tasdiq` — no match, and an entire fake-parcel campaign walked through.
Folding now merges `a` and `e`, so all three spellings collapse to one form. One character
was costing a whole campaign.

## What these numbers do not say

Being straight about the limits, because the limits are real:

- **Part of the corpus is translated.** A share of the `smishing` class was clearly localised
  from an older English SMS spam collection — Nokia 3510i offers, UK `08`/`09` numbers, PO
  boxes, "your 2003 statement". Those are genuine scam *scripts* and the detector should catch
  them, but they are not evidence of current Azerbaijani campaigns. The Azerbaijan-native rows
  (fake Azərpoçt parcels, fake tax debt, fake state support, bank card suspension) are the part
  that speaks to the local threat.
- **Phone numbers in the corpus are redacted** as `08XXXXXXXX`. The phone pattern therefore
  accepts `X` alongside digits. In the wild it sees real digits, which it also matches, but
  this has not been measured on unredacted traffic.
- **I tuned the weights by hand while reading the DEV half.** A human looking at data is a
  learning process with more capacity than the small weight sweep suggests. The DEV-to-TEST
  gap is the only honest estimate of how much that cost.
- **The `spam` class is not the target.** It alarms on about half of the spam messages. That is
  neither good nor bad here: unwanted marketing is not what this app exists to stop.
- **One corpus is one corpus.** 90.3% on this data is not a promise about next month's
  campaigns. The rules will need updating as the scripts change, and `MEASUREMENT.txt` is
  regenerated by the test so any change in the numbers shows up.
- **Nothing here was tested on live incoming SMS at scale.** The end-to-end path (receive,
  score, explain, notify a family member) has been exercised on a real device, message by
  message; it has not been run against a month of one person's real traffic.

## Reproducing the figures exactly

`./gradlew :app:testFullDebugUnitTest --tests '*CorpusMeasurementTest*'` writes `MEASUREMENT.txt` next to this file.
The committed copy of that file is the output of the run described above. If your numbers
differ, the corpus has changed upstream or the rules have been edited — both worth knowing.

---

## What it still misses

On the held-out TEST half the detector misses 29 smishing messages. Every one of them
scores below the alarm threshold of 3 — fourteen score 0, eight score 1, seven score 2.
None of them is a close call from the wrong side of the line. They are messages where
too few rules fire at all.

| what fired on the 29 missed messages | count |
|---|---|
| `INSECURE_HTTP` | 7 |
| `PRIZE_LURE` | 5 |
| `URGENCY` | 3 |
| `PRIZE_CLAIM` | 2 |
| nothing fired at all | 14 |

Reading the 29, the gap is not one hole but five:

1. **Short codes and prices in wording the rules do not cover.** `PREMIUM_SHORTCODE`
   matches *"HƏ yazıb 85023-ə göndərin"* but not *"ŞKL sözünü 89080 nömrəsinə göndərin"*.
   `PREMIUM_RATE` matches a per-minute price (*20qəp/dəq*) but not a flat one
   (*Qiymət: 1.50AZN*). Four missed messages contain one of these and were not scored for it.
2. **Invented or misspelt top-level domains** — `.nett`, `.bizz`, `.azz`, `.co.cc`.
   There is no rule for a TLD that does not exist.
3. **A digit substituted into a plausible domain** — `southbankm0saics.az`, with a zero
   for the letter o. The punycode rule does not cover Latin digit substitution.
4. **A bank's name plus a payment demand and no link at all.** Two missed messages name
   Kapital Bank and give a callback number. `BODY_BRAND_MISMATCH` compares the named
   institution against the link host, so with no link there is nothing to compare.
5. **An `.apk` download link.** One missed message offers a bank app "security update"
   as a direct `.apk`. The file extension is not scored.

## Ten of the misses cannot be caught on this corpus at all

Ten of the 29 contain a phone number the dataset has redacted: `08XXXXXXXXX`,
`07XXXXXXX`, `99XXXXXXXXX`. `CALLBACK` and `PREMIUM_RATE` look for digits, and there are
no digits left to find. No version of this detector can score those messages on this
corpus, because the evidence was removed before the data was published.

That is 34% of the remaining gap. The reachable ceiling here is well below 100%, and the
distance has nothing to do with the rules.

## Where the false alarms come from

29 legitimate messages are flagged in the TEST half.

| signal present on a false alarm | count |
|---|---|
| `URGENCY` | 17 |
| `UNKNOWN_LINK_ACTION` | 8 |
| `PREMIUM_SHORTCODE` | 3 |
| `BODY_BRAND_MISMATCH` | 2 |
| `SHORTENER` | 2 |
| `CALLBACK` | 2 |
| `BRAND_MISMATCH`, `SENDER_MISMATCH`, `INSECURE_HTTP`, `PRIZE_LURE` | 1 each |

**Eleven of the 29 are caused by `URGENCY` and nothing else.** Some of them are ordinary
messages between people:

> *"Salam, mahnıları hansı saytdan yükləyim, təcilidir, xahiş edirəm de."*
> *"Zəhmət olmasa mənə zəng edə bilərsən? ... Təcili zəng etməliyəm."*

Others are real bank notifications that are urgent because the situation is urgent — an
ABB one-time code, an AccessBank dormancy notice. `URGENCY` fires at 44.9% on smishing
and 3.8% on legitimate messages, so it earns its place, but it is the single largest
source of noise and the next thing to split into finer signals.

**The worst false alarm scores higher than every scam the detector missed.** Row 114 is a
genuine Azercell message confirming a number order. It links to `www.azercellim.com`, a
real Azercell domain whose name does not match the brand string, so `BRAND_MISMATCH`,
`BODY_BRAND_MISMATCH` and `SENDER_MISMATCH` all fire at once: **score 11, DANGEROUS**.
The highest-scoring missed scam scored 2.

This says something the headline numbers do not: the score is not calibrated at the top
end. A high score means *many rules agreed*, not *more likely to be fraud*. Three brand
rules looking at the same mismatch are not three pieces of evidence.

## What this corpus cannot say about Russian

Azerbaijan is bilingual and the detector reads only one of its languages. That gap is
real, but it cannot be measured here: of 4,538 rows, **8 contain any Cyrillic text, and
exactly one of those is smishing.** Adding Russian keyword rules to this detector today
would produce a feature whose effect could not be shown either way. Collecting Russian
scam SMS has to come before writing Russian rules.

## Why none of this is fixed in this version

Every number quoted in this repository, in the demo video and on the Devpost entry comes
from one run of the detector as it stands. Changing a rule changes the numbers, and a
number that appears in four places and is corrected in three is worse than one that is
left alone. These findings are the next version's work list, published before that
version exists.