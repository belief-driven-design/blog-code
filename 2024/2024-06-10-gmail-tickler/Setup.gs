// AGGREGATE LABEL CREATION UNDER A SINGLE OBJECT, SO THE FUNCTIONS
// CAN'T BE RUN VIA THE TOOLBAR
const Label = {

  // UPSERTS A SINGLE LABEL
  upsert: (prefix, value) => {
    const name = Tickler.labelName(prefix, value)
    let label = GmailApp.getUserLabelByName(name)

    if (!!label) {
      return
    }
    GmailApp.createLabel(name)
  },

  // UPSERTS MULTIPLE LABEL BASED A NUMERIC RANGE
  upsertRange: (prefix, from, to) => {
    for (let val = from; val <= to; val++) {
     Setup.createLabel(prefix, val)
    }
  } 
}

// THE IDEMPOTENT RUN-ONCE SETUP FUNCTION
const run_once_setup = () => {

  // STEP 1: create labels

  Label.create()

  Label.createRange(Prefix.HOUR, 0, 23)
  Label.createRange(Prefix.DAY, 1, 31)
  Label.createRange(Prefix.MONTH, 1, 12)

  const year = new Date().getFullYear()
  createLabels(Prefix.YEAR, year, year + 3)

  Label.create(Prefix.NEXT)
  Label.create(Prefix.NEXT, Prefix.DAY)
  Label.create(Prefix.NEXT, Prefix.WEEK)
  Label.create(Prefix.NEXT, Prefix.WEEKEND)
  Label.create(Prefix.NEXT, Prefix.MONTH)
  Label.create(Prefix.NEXT, Prefix.YEAR)


  // STEP 2: (re)create triggers

  // DELETE ALL EXISTING TRIGGERS
  ScriptApp.getProjectTriggers().forEach(ScriptApp.deleteTrigger)
  
  // HOURLY TRIGGER: EVERY 10 MINUTES
  ScriptApp.newTrigger(processHourly.name)
    .timeBased()
    .everyMinutes(10)
    .create()

  // DAILY TRIGGER: AT 03:30
  ScriptApp.newTrigger(processDaily.name)
    .timeBased()
    .atHour(3)
    .nearMinute(30)
    .everyDays(1)
    .create()

  // MONTHLY TRIGGER: AT 1ST
  ScriptApp.newTrigger(processMonthly.name)
    .timeBased()
    .onMonthDay(1)
    .create()
}
