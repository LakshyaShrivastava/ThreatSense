print("Started running mongo-init.js")

try {
  db.getSiblingDB('network_logs_db').createUser({
    user: 'mongoadmin',
    pwd: 'password',
    roles: [
      {
        role: 'readWrite',
        db: 'network_logs_db',
      }, 
    ],
  });
  print('User "mongoadmin45" created successfully.');
} catch (e) {
  print(e);
}

print("Completed mongo-init.js")
