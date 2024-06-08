// LABEL PREFIXES
const Prefix = {
  ROOT: 'ZZZ',
  HOUR: 'H',
  DAY: 'D',
  WEEK: 'W',
  WEEKEND: 'WE',
  MONTH: 'M',
  YEAR: 'Y',
  NEXT: 'N'
}


const Tickler = {

  labelName: (prefix, value) => {
    if (prefix === undefined) {
      return Prefix.ROOT
    }

    if (value === undefined)  {
      return `${Prefix.ROOT}/${prefix}`
    }
  },

  moveToInbox: (labelPrefix, labelValue) => {
    try {
      // STEP 1: GET LABEL
      const labelName = Ticker.labelName(labelPrefix, labelValue)
      const label = GmailApp.getUserLabelByName(labelName)
      if (!label) {
        Logger.log(`Label '${labelName}' not found`)
        return
      }

      // STEP 2: GET ASSOCIATED THREADS
      const threads = label.getThreads()
      if (threads.length === 0) {
        return
      }

      // STEP 3: MOVE THREADS, MARK AS UNREAD
      GmailApp.moveThreadsToInbox(threads)
      GmailApp.markThreadsUnread(threads)

      // STEP 4: REMOVE THREADS FROM TICKLER LABEL
      label.removeFromThreads(threads)
    }
    catch (e) {
      const msg = `Moving '${labelName}' failed`
      Logger.log(msg)
      GmailApp.sendEmail('johndoe@example.com', `[TICKLER-ERROR] ${msg}`, msg)
    }
  }
}
