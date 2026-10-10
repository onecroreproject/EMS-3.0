const { MongoClient } = require('mongodb');

async function run() {
  const uri = "mongodb+srv://gsanthosh09112000_db_user:jCYaGVyJsMrTxlr7@ems.o0yy1tu.mongodb.net/employee";
  const client = new MongoClient(uri);
  try {
    await client.connect();
    const db = client.db('employee');
    const col = db.collection('admin_config');
    await col.deleteMany({});
    console.log("Deleted admin_config successfully.");
  } finally {
    await client.close();
  }
}
run().catch(console.error);
