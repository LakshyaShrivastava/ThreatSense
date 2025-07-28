const rootUsername = process.env.MONGO_INITDB_ROOT_USERNAME;
const rootPassword = process.env.MONGO_INITDB_ROOT_PASSWORD;
const appDbName = "network_logs_db";
const appUsername = "mongoadmin";
const appPassword = "password";

print("Started running mongo-init.js");

try {
    // 1. Authenticate as the root user first (this session is running with root privileges)
    db.getSiblingDB('admin').auth(rootUsername, rootPassword);

    // 2. Switch to the application database (or get a handle to it)
    const appDb = db.getSiblingDB(appDbName);

    // 3. Create the application user with readWrite role for that specific database
    appDb.createUser(
       {
         user: appUsername,
         pwd: appPassword,
         roles: [ { role: "readWrite", db: appDbName } ]
       }
    );

    print(`Successfully created user '${appUsername}' in database '${appDbName}'.`);
} catch (e) {
    print(`Error creating user: ${e}`);
}

print("Completed mongo-init.js");

