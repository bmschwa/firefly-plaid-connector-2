# Firefly Plaid Connector 2
Connector to pull Plaid financial data into the Firefly finance tool.

Inspired by [firefly-plaid-connector](https://gitlab.com/GeorgeHahn/firefly-plaid-connector/).

# Running
Please note this version of the connector requires at least version v6.1.2 of Firefly.

These are basic instructions for installing and running the connector. See further topics below for more details.

## Running the JAR Directly 
1. Ensure you have a JRE or JDK for at least Java 17.
2. Download the latest JAR from the [releases page](https://github.com/dvankley/firefly-plaid-connector-2/releases).
3. Move the JAR to your desired working directory. 
4. Make a `persistence/` subdirectory in your working directory for the connector to persist data to that's writeable 
by the user running the connector.
5. Set up a configuration file. I suggest copying the existing `application.yml` and modifying as needed.
See [below](#configuration) for details on configuration.
6. Run the connector, for instance with `java -jar connector.jar --spring.config.location=application.yml`

## Running via Docker
New versions of the Docker image are pushed to GHCR with each release.
The latest version is available at `ghcr.io/dvankley/firefly-plaid-connector-2:latest`.

You can also build your own with `./gradlew bootBuildImage --imageName=your-docker-registry/firefly-plaid-connector-2`.

### Docker Compose
1. Pull down the [docker-compose-polled.yml](https://raw.githubusercontent.com/dvankley/firefly-plaid-connector-2/main/docker-compose-polled.yml) and/or
[docker-compose-batch.yml](https://raw.githubusercontent.com/dvankley/firefly-plaid-connector-2/main/docker-compose-batch.yml) files.
   1. Ensure you copy the raw file with the exact same whitespace, otherwise you'll have issues. YAML is very picky about whitespace.
   2. The two different compose files correspond to different [run modes](https://github.com/dvankley/firefly-plaid-connector-2/blob/main/README.md#mode)
   and contain suggested configuration for a given mode.
2. Set the `HOST_APPLICATION_CONFIG_FILE_LOCATION` environment variable to point to your
[config file](https://github.com/dvankley/firefly-plaid-connector-2/blob/main/README.md#configuration-file)
(or just manually insert your path in the compose file).
3. If running in polled mode (and thus using `docker-compose-polled.yml`), create a directory on your host machine
that is writable by anyone, then pass its location in as `HOST_PERSISTENCE_DIRECTORY_LOCATION`.
   1. This directory will store the polled mode cursor file, which is basically the polled mode's last synced state.
   2. Using a bind mount for this is kind of crappy, but I couldn't find a good way to make the Spring Boot Gradle
   bootBuildImage plugin set perms on a named volume cleanly. Suggestions welcome.
4. Run `docker compose up`.
### Docker CLI
#### Requirements
The application container requires the following:
1. An application configuration file to read from.
   1. See [below](#configuration) for details on configuration.
   2. The `SPRING_CONFIG_LOCATION` environment variable can be 
   used to set the location (in the container) of the application configuration file.
2. A directory to write the sync cursor file to (if run in polled mode).
   1. The `FIREFLYPLAIDCONNECTOR2_POLLED_CURSORFILEDIRECTORYPATH` environment variable can be
      used to set the location (in the container) of the directory that will contain the sync cursor file.

The example below uses bind mounts for these purposes for
simplicity, but you can also use volumes if you want.
If using volumes you will need to use an intermediate container
to write the application configuration file to a volume, as well as setting permissions to allow the
application user `cnb` to write to the cursor file directory's volume.

#### Example Docker Run Command
```shell
docker run \
--mount type=bind,source=/host/machine/application/config/file/directory,destination=/opt/fpc-config,readonly \
--mount type=bind,source=/host/machine/writeable/directory,destination=/opt/fpc-cursors \
-e SPRING_CONFIG_LOCATION=/opt/fpc-config/application.yml \
-e FIREFLYPLAIDCONNECTOR2_POLLED_CURSORFILEDIRECTORYPATH=/opt/fpc-cursors \
-t firefly-plaid-connector-2
```

# Concepts
## Mode
The connector can be run in either `batch` or `polled` mode.
### Batch
Uses the [Plaid transactions get](https://plaid.com/docs/api/products/transactions/#transactionsget) endpoint to
pull historical Plaid transaction data, convert it to Firefly transactions, and write them to Firefly.

This is typically used when you're first setting up your Firefly instance and you want to backfill existing transactions
before running indefinitely in `polled` mode.

`Batch` mode can be memory intensive if you're pulling a large volume of transactions, so if your Firefly server is
running on a low-spec VPS (like mine), then I recommend running your large `batch` mode pull on a higher spec machine
(like your home computer or whatever). It can still be pointed at your existing Firefly server with no problem.

### Polled
Uses the [Plaid sync](https://plaid.com/blog/transactions-sync/) endpoint to periodically pull down transaction data,
convert it to Firefly transactions, and write them to Firefly.

When started in this mode, the connector will not pull any past transactions, but will check for new transactions every 
`fireflyPlaidConnector2.polled.syncFrequencyMinutes`.

The last sync position of each account will be stored in `persistence/plaid_sync_cursors.txt`.

## Firefly Transfers
The most complex part of the connector by far is the transfer matching logic.
Firefly has a notion of [transfers](https://docs.firefly-iii.org/firefly-iii/concepts/transactions/#transfers),
which are special transactions that instead of the usual flow of money from an asset account into an expense account
or from a revenue account into an asset account, represent the flow of money from one asset account into another.

For me this typically happens when I'm making payments on credit cards or the like (because I have credit cards set
up as asset accounts instead of liability accounts because that's how Firefly works).

Plaid has no notion of inter-account transactions; transaction data only references the account that it's on.

We try to bridge these by matching two Plaid transactions on different asset accounts and converting them into a single
Firefly transfer. You can see the full logic in `TransactionConverter.sortByPairs`, but the summary is that the
connector searches for pairs of Plaid transfer-type transactions with inverse amounts (i.e. $100 and -$100)
that are within `fireflyPlaidConnector2.transferMatchWindowDays` of each other. This isn't flawless, but
is sufficient in most cases.

Note that if the connector attempts to convert an existing non-transfer Firefly transaction and an incoming Plaid
transaction to a Firefly transfer, the existing Firefly transaction will be deleted and the new transfer
transaction will be created because the Firefly API does not support converting existing transaction types.

## Initial Balances
**Note**: Given that the balance endpoint is now $0.10 per call, this feature isn't really worth using anymore.

If `fireflyPlaidConnector2.batch.setInitialBalance` is set to `true`, the connector will try to create "initial balance"
transactions for each Firefly account that result in the current Firefly balance for each account equalling the
current balance that [Plaid reports](https://plaid.com/docs/api/products/balance/#accountsbalanceget) for that account.
This is determined by summing up all transactions pulled during
this run for that account, then subtracting that from the Plaid-reported current balance to get the amount for the 
"initial balance" transaction.

For Firefly asset accounts that are of the "credit card" type, the Plaid balance is interpreted as a negative rather
than a positive value. This is because Plaid always reports balances as positive (as far as I can tell), and it's
assumed that a Firefly account set as a credit card is linked to a Plaid account that's also a credit card.

Ensure that the current balance in the target Firefly accounts are 0 before using this feature.

Because the initial balance transaction amount is determined from the transactions pulled in this batch, I do not
recommend enabling this feature unless you're pulling all of the transactions you intend to backfill for a given
account (as opposed to just filling in some gaps).

Note that the [Plaid balance endpoint](https://plaid.com/docs/api/products/balance/#accountsbalanceget)
(or the underlying institution data source) is kind of crappy. To wit:
* There is a `lastUpdatedDatetime` field in the response schema, but according to the Plaid documentation it's only
populated for Capital One for whatever reason,
and due to that it's impossible to tell exactly what point in time the Plaid balance represents.
  * The upshot of this is that if the Plaid balance is out of date by a transaction or two,
  the "initial balance" transaction amount will also be a bit off. Plan accordingly.
* According to the docs, the Plaid balance endpoint forces a synchronous update from the underlying institution data
source, which can cause this part of the connector's run to take a while. The Plaid API timeout has been
[adjusted](https://github.com/dvankley/firefly-plaid-connector-2/blob/a73fb692937984b64af34a8047c05fcef03cc088/src/main/kotlin/net/djvk/fireflyPlaidConnector2/api/ApiConfiguration.kt#L29)
to account for this, but it still fails with surprising regularity.

# Configuration
## Plaid
As you have probably guessed from the name, Plaid is an important part of using this connector.
To use Plaid, you will need an account.

### Environments
Once upon a time, you could use up to 100 items in the Plaid development environment for free, but that is no longer the case.

If you're a new user, you will need to sign up for production environment access. This is a relatively painless process
to go through, but of course the main downside is that you have to pay for it. Plaid bills $0.30/institution/month for
the `transactions` product, which is all you need for the connector to work (unless you enable [initial balances](#initial-balances),
which I feel like doesn't make sense anymore given the billing model).
Note that this is per institution, not per account. For instance if you have 2 accounts on one institution, you will
only be billed $0.30 a month.

If you're an existing user of the development environment, you _may_ be able to get away with "free limited Production access"
per [this comment](https://github.com/dvankley/firefly-plaid-connector-2/issues/67#issuecomment-2077358653), but I
wouldn't recommend planning on that.

If you're an existing user of the development environment and looking to migrate to the production environment, see
the [section on migration](#migrating-from-development-to-production).

### Oauth Registration
If you want access to US Oauth institutions, you need to jump through [some additional hoops](https://dashboard.plaid.com/settings/compliance/us-oauth-institutions).
Based on my brief conversation with Plaid support and my personal experience, you can skim through the company info
and security questionnaire pretty quickly, using dummy names, logos, and answering "I'm a hobbyist using this for personal use"
for all the security questions.

Once you've completed all the requirements for Oauth access, the applicable institutions should just work in Quickstart
(see below).

### Basic Credentials
Once you've signed up for Plaid, you should be provided a client id and secret, which go in the application config
file where you'd expect (`fireflyPlaidConnector2.plaid.clientId` and `fireflyPlaidConnector2.plaid.secret`).

### Connecting Accounts
Next up, you need to connect Plaid to your various financial institutions. The easiest way to do this is to run
[Plaid Quickstart](https://plaid.com/docs/quickstart/) locally. I found the Docker experience to be fairly painless.
I recommend copying the `.env.example` file to a new `.env` file, filling in your credentials, setting `PLAID_ENV` to
`development` or `production`, and setting `PLAID_PRODUCTS` to `transactions`.
Note that you will need to add `balances` to use the [initial balances](#initial-balances) feature, but given that
there's an additional cost for that in production, it's not really worth it.
Note that
if you leave `PLAID_PRODUCTS` set to the default `auth,transactions`, you won't be able to connect to some of the
financial institutions you might expect because they don't support the `auth` product.

Once you have Quickstart running, just follow the UI prompts to connect to your financial institutions.
For each institution you connect to, Plaid should give you an `item_id` and an `access_token`. Make a note of both.
Each institution can contain multiple accounts (i.e. your bank has both your savings and checking account), so you
will need to make an additional call to the Plaid API to get individual account ids. Example callout using `httpie`:
```
http POST https://production.plaid.com/accounts/get \
    client_id=yourclientid \
    secret=yoursecret \
    access_token=access-production-your-items-access-token
```
This will give you a list of account ids that belong to that item/financial institution. Make a note of these, as
you will need to enter them into the connector's configuration file.

## Configuration File
The configuration file is unsurprisingly where most of the configuration of the connector takes place.
I recommend copying the [`application.yml` file](https://github.com/dvankley/firefly-plaid-connector-2/blob/main/src/main/resources/application.yml)
and customizing it to your preferences. Each property should be well documented in the comments of that file.

# Reference Workflow
This is my workflow for using the connector with Firefly.
Of course, you don't have to use it exactly like this, but you may find this useful for reference in thinking through your own workflow.

1. Set up your Plaid developer account, connect to your various financial institutions, and set up the connector
application configuration file as described in [Configuration](#configuration).
2. Plan your category mapping
   1. Determine what Firefly [budgets](https://docs.firefly-iii.org/firefly-iii/concepts/budgets/)
   and [categories](https://docs.firefly-iii.org/firefly-iii/concepts/categories/) you want to use.
      1. Note the [functional difference](https://docs.firefly-iii.org/firefly-iii/concepts/budgets/#the-difference-between-budgets-and-categories) between the two.
      2. In my case, as recommended by the Firefly documentation, we used budgets for expenses that are optional and
      should have a limit (or target) amount each month. We used categories for expenses that are not optional or
      should not have a limited amount each month.
      3. You may want to just put everything in budgets (and not set amounts for categories that don't need them)
      as it makes the charts and reports a bit easier to read.
   2. Determine the mapping between your Firefly budgets/categories and Plaid categories.
      1. The Plaid category taxonomy includes "primary" categories and "detailed" subcategories.
         1. See the [official reference CSV](https://plaid.com/documents/transactions-personal-finance-category-taxonomy.csv)
         2. Or if you prefer, see the connector's [parsed category list](https://github.com/dvankley/firefly-plaid-connector-2/blob/a31977ff28261593966df66e0e5ba6da07db9746/src/main/kotlin/net/djvk/fireflyPlaidConnector2/api/plaid/models/PersonalFinanceCategoryEnum.kt#L159).
      2. Assign Plaid primary or detailed categories to your Firefly budgets and categories.
         1. Each Plaid primary or detailed category may only be assigned to one Firefly budget or category.
         2. Multiple different Plaid primary or detailed categories may be assigned to a single Firefly budget or category.
            1. For example, you might assign both `FOOD_AND_DRINK.BEER_WINE_AND_LIQUOR` and `ENTERTAINMENT` to a 
            "Spending" budget.
         3. You can assign both a Plaid primary category and its detailed subcategories to different Firefly budgets
         or categories; just make a careful note in these cases as you have to be careful when building the corresponding
         Firefly rules later.
3. Implement your Firefly budgets/categories and corresponding rules
   1. Creating budgets and categories is straightforward. You do not need amounts for budgets at this time.
   2. Rules
      1. I created a rule group for Plaid category processing, then another rule group below it (and thus overriding it)
      to handle case-by-case overrides of specific transactions that Plaid didn't categorize the way I wanted.
      2. For Plaid category processing, my basic rule template is:
         1. Trigger: when a transaction is created
         2. Stop processing: `true`
         3. Strict mode: `false`
         4. Rule Triggers: "Any tag is...": `plaid-detailed-cat-coffee` etc.
            1. Add additional tag triggers if multiple Plaid categories are assigned to a single Firefly budget or category
         5. Action: "Set Budget (or Category) to...": corresponding Firefly budget or category name
      3. Consider using [@514amir's] [automated rule setup script](https://github.com/dvankley/firefly-plaid-connector-2/blob/main/plaidfireflyrulecreator.sh)
      to simplify this step.
4. Save a snapshot of your Firefly database state in case the next steps don't do what you want
   1. This step is optional but recommended.
   2. If Firefly's using a Sqlite database, all you have to do is copy the database file. If using Mysql or Postgres,
   you will need to use the appropriate backup and restore tooling.
5. Run the connector in `batch` mode
   1. I recommend doing this connector run on a higher-spec machine (i.e. your laptop or whatever) to give it all the
   memory it needs. It doesn't matter where you run it as long as the connector can make a network connection to your
   Firefly server.
   2. The `fireflyPlaidConnector2.batch.maxSyncDays` property is up to you. I used 2 years for my initial backfill,
   but your value will depend on your needs and patience for the process.
      1. For what it's worth, the performance bottleneck is Firefly handling transaction inserts. This isn't a system
      resource thing either, as it took about as long on my M1 Max as it did on a $5 VPS.
   3. Keep an eye on the connector's logs to ensure nothing's gone wrong.
6. Go through transaction reports and add additional rules for any categorization gaps
   1. I went through transactions without budgets for each month and added new override rules as needed.
      1. Transactions without budgets in Firefly can be viewed by navigating to Budgets in the sidebar, selecting
      the desired month in the Period Navigator at the top, then clicking the "Expenses without budget" link in the
      very bottom left of the window.
   2. The "Apply rule X to a selection of your transactions" feature in the Firefly Automations UI is very useful
   for filling in budget/category gaps after you've already run a transaction import.
7. If anything's gone wrong, restore your database snapshot from before and try again.
8. Set up the connector to run in `polled` mode.
   1. I created a `systemd` unit to run the connector on my Debian VPS, your mileage may vary.
      1. You will of course need to set up users, permissions, directories, etc. to support this.

```systemd
[Unit]
Description=Firefly Plaid Connector 2

[Service]
WorkingDirectory=/opt/firefly-plaid-connector
ExecStart=/bin/java -Xms128m -Xmx1024m -jar firefly-plaid-connector.jar --spring.profiles.active=prod
User=firefly-plaid-connector
Type=simple
Restart=on-failure
RestartSec=10
Environment=“SPRING_CONFIG_LOCATION=/opt/firefly-plaid-connector/application-prod.yml”

[Install]
WantedBy=multi-user.target

```
# Credential Updates
   On occasion, you will get an `ITEM_LOGIN_REQUIRED` error in the connector logs. This typically happens when the credentials
   for one of the institutional accounts you've linked Plaid to have changed. You can find the access token for the account in question on the log line above the exception log.
   There are two methods for resolving the error, described below.
## Update Mode
This is the recommended method of resolving this issue, although it's a bit more complex than create mode.
1. Check out https://github.com/dvankley/quickstart
2. Start the frontend and the java backend per the instructions in the README and verify that the frontend loads correctly at `https://localhost:3000`.
3. Find the access token value for the account with the `ITEM_LOGIN_REQUIRED` error (it should be in the connector logs just above the exception).
4. Navigate to `https://localhost:3000?input_access_token=$yourAccessTokenHere` in your browser.
5. Complete the Link flow in the UI.

After completing these steps, your account credentials should be fixed and the connector should resolve itself on its next run (assuming you're running in polled mode).

## Create Mode
This is basically just going through the [Connecting Accounts](https://github.com/dvankley/firefly-plaid-connector-2#connecting-accounts)
   workflow again for that account, replacing the access token and account id in your configuration file, and restarting the connector.
   Unfortunately, as discussed in
   https://github.com/dvankley/firefly-plaid-connector-2/issues/39, this method permanently chews through your Item quota and is _NOT RECOMMENDED_ for that reason.
   At this point it does have the advantage of being simpler than update mode, which is why it's listed here as an option.

## Possible Causes of Frequent Occurrence
If you're getting this error frequently, check if you have MFA enabled on your account with the corresponding financial institution. MFA can cause
   high frequency invalidation of Plaid account credentials, so consider disabling it. Obviously compromising your security posture to use this connector
   isn't great, so hopefully your institution provides limited permission accounts for service access.
   
   Also note that CIBC currently has [this issue](https://github.com/dvankley/firefly-plaid-connector-2/issues/39#issuecomment-1817557063).

# Migrating from Development to Production
If you've been using the `development` environment for a while and are now being compelled to move to `production`, here's
a checklist of the steps I followed to perform this migration that might be helpful.

1. Submit Plaid request for production access and wait for approval.
2. If you're in the US and want to use OAuth institutions, work through the list required for OAuth institutions.
   1. You can basically put "I'm just a hobbyist" for all the security etc. questions and they'll approve you.
3. Change your quickstart .env file to point to the production environment and update it with your new secret (your client id is probably the same).
4. Run the link flow through quickstart for all items you want to migrate, the same as you did initially for the development environment.
5. Stop the connector process if you're running it in polling mode somewhere.
6. Back up your production Firefly database.
7. Update your application config file.
   1. `plaid.url` should be https://production.plaid.com
   2. `plaid.secret` needs to be updated to your new secret.
   3. Each account should be updated with the new access token and plaid account id. Keep the same firefly account ids if you want to migrate in place.
8. If (like me) your connector's been down for a while, run the connector in batch mode to cover the time range from your last successful sync from Plaid.
9. Delete the cursor file in your persistence directory
   1. I had thought maybe I could update the access tokens in my cursor file and things would just work, but turns out
   Plaid couples the cursors values to their access tokens, so they don't cross over between environments.
10. Run the connector in polled mode per usual.
11. Take a close look at the time range when you switched from development to production and clean up any duplicates you find.
    1. The connector's update functionality won't properly bridge environments because the ids are different, so you may see some transactions around
    that time that should have been updated end up being duplicated. Just delete the old ones and hopefully you should be good going forward.
12. Profit

# Troubleshooting
## Known Issues
* When setting up access to a provider from the Plaid Quick Start I'm getting a message "Something went wrong".
   * Several institutions are restricting access to development access accounts.
   An approved paid production account will need to be setup with Plaid to gain access to these accounts.
   * This can also surface as `INSTITUTION_NO_LONGER_SUPPORTED` or `UNAUTHORIZED_INSTITUTION` errors.
 
* I'm getting an `ITEM_LOGIN_REQUIRED` error when running the connector.
   * See https://github.com/dvankley/firefly-plaid-connector-2#credential-updates 

## New Issues
### Logs
The `logging` key in the application configuration file controls the logging level of various packages and classes in
the application. Setting `logging.level.net.djvk` to `DEBUG` or `TRACE` is recommended if you're having problems,
as it will log additional info that should help diagnose the issue.

### Reporting Issues
If you have an issue, feel free to report it via the Github issue tracker. I am actively maintaining this project
but my available time is finite, so the odds of your issue being addressed will be increased if you include relevant
logs at the `TRACE` level with your issue report.
Writing a test case (i.e. in `net.djvk.fireflyPlaidConnector2.transactions.TransactionConverterTest`) to demonstrate
your issue greatly increases the chances of me fixing it quickly. If test infrastructure is missing to test the
element of the code you see an issue with, let me know and I can work on improving that.

# Development
## Setup
Setup should be identical to any other Spring Boot/Gradle application.
I recommend adding an additional configuration file (i.e. `application-dev.yml`) and enabling the corresponding Spring
profile (i.e. `dev`) to allow you to persist and iterate on your local configuration.

I recommend setting up a local copy of Firefly for development purposes, especially one that you can easily backup
and restore the database for to minimize your feedback loop on testing things.

# FAQ
* Why did you make a new program rather than contributing to [firefly-plaid-connector](https://gitlab.com/GeorgeHahn/firefly-plaid-connector/)?
  * I initially tried firefly-plaid-connector, but I had a few issues with it, and it didn't fully support Plaid categories.
I tried to set it up for development locally, but after about an hour trying to get the right version of the .NET SDK to work,
I decided I was better off making my own gravy. So here we are.

# Other Resources
## Budget Notifications via Home Assistant
Do you, like me, run Home Assistant and also want real-ish time budget notifications? Then
check out [this Node-RED](https://github.com/dvankley/homeassistant-firefly-budget-alert) flow.
