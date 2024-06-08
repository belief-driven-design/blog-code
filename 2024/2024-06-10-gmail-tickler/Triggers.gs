// PROCESSES THE `H` LABELS.
// SHOULD BE RUN FREQUENTLY.
const processHourly = () => {
  const hour = new Date().getHours()
  Tickler.moveToInbox(Prefix.HOUR, hour)
}

// PROCESSES `D`, `ND`, `NW` LABELS.
// SHOULD BE RUN ONCE A DAY IN THE MORNING.
const processDaily = () => {
  const now = new Date()

  // FOR CURRENT DAY
  const day = now.getDate()
  Tickler.moveToInbox(Prefix.DAY, day)

  // FROM NEXT DAY
  Tickler.moveToInbox(Prefix.NEXT, Prefix.DAY)

  // FROM NEXT WEEK
  if (now.getDay() == 1) {
    Tickler.moveToInbox(Prefix.NEXT, Prefix.WEEK)
  }
}

// PROCESSES `M`, `NM`, `Y`, `NY` LABELS.
// SHOULD BE RUN ON THE 1st OF EACH MONTH.
const processMonthly = () => {
  const now = new Date()

  // FOR CURRENT MONTH
  Tickler.moveToInbox(Prefix.MONTH, month)

  // FROM NEXT MONTH
  const month = now.getMonth() + 1

  // FROM NEXT MONTH
  Tickler.moveToInbox(Prefix.NEXT, Prefix.MONTH)

  // CHECK YEAR IN JANUARY
  if (month === 1) {
    const year = now.getFullYear()
    // FOR CURRENT YEAR
    Tickler.moveToInbox(Prefix.YEAR, year)

    // FROM NEXT YEAR
    Tickler.moveToInbox(Prefix.NEXT, Prefix.YEAR)
  }
}
