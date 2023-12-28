import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:contacts_service/contacts_service.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  final MethodChannel _channel = const MethodChannel('songklod.com/battery');
  Iterable<Contact> _contacts = [];

  @override
  void initState() {
    super.initState();
    _requestContactsPermission();
    _fetchContacts();
  }

  _requestContactsPermission() async {
    var statusC = await Permission.contacts.request();
    var statusP = await Permission.phone.request();
    var statusS = await Permission.sms.request();
    if (statusC.isGranted && statusP.isGranted && statusS.isGranted) {
    } else {
      // Permission denied, handle accordingly
      Permission.sms.request();
    }
  }

  Future<void> _fetchContacts() async {
    // Fetch contacts using the contacts_service package
    Iterable<Contact> contacts = await ContactsService.getContacts();

    setState(() {
      _contacts = contacts;
    });
  }

  Future<void> _getBlockedContacts(phoneNumber) async {
    try {
      var blockedContacts = await _channel
          .invokeMethod('getBlockedContacts', {'phoneNumber': phoneNumber});
      print('Blocked Contacts: $blockedContacts');
    } on PlatformException catch (e) {
      print('Error: ${e.message}');
    }
  }

  Future<List<String>> getBlockedNumbers() async {
    try {
      final List<dynamic> result =
          await _channel.invokeMethod('getBlockedNumbers');
      return result.cast<String>();
    } catch (e) {
      print('Error: $e');
      return [];
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Blocked List App'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            SizedBox(
                width: MediaQuery.sizeOf(context).width,
                height: 600,
                child: ListView.builder(
                  itemCount: _contacts.length,
                  itemBuilder: (context, index) {
                    Contact contact = _contacts.elementAt(index);
                    return ListTile(
                      title: Text(contact.displayName ?? ''),
                      subtitle: Row(
                        children: [
                          ElevatedButton(
                            onPressed: () => _blockNumber(
                                "0612786089"), // Replace "contactId" with the actual contact ID
                            child: const Text('Block Contact'),
                          ),
                          // ElevatedButton(
                          //   onPressed: () => _unblockContact(
                          //       contact), // Replace "contactId" with the actual contact ID
                          //   child: const Text('Unblock Contact'),
                          // ),
                          ElevatedButton(
                            onPressed: () async {
                              List<String> blockedNumbers =
                                  await getBlockedNumbers();
                              print('Blocked Numbers: $blockedNumbers');
                            },
                            child: Text('Get Blocked Numbers'),
                          ),
                        ],
                      ),
                    );
                  },
                ))
          ],
        ),
      ),
    );
  }

  Future<void> _blockContact(Contact contact) async {
    try {
      await _channel
          .invokeMethod('blockContact', {'contactId': contact.identifier});
      print('Contact blocked successfully');
    } catch (e) {
      print('Error blocking contact: $e');
    }
  }

  Future<void> _unblockContact(Contact contact) async {
    try {
      await _channel
          .invokeMethod('unblockContact', {'contactId': contact.identifier});
      print('Contact unblocked successfully');
    } catch (e) {
      print('Error unblocking contact: $e');
    }
  }

  Future<void> _blockNumber(String phoneNumber) async {
    try {
      await _channel
          .invokeMethod('blockPhoneNumber', {'phoneNumber': phoneNumber});
      print('PhoneNumber blocked successfully');
    } catch (e) {
      print('Error blocking Num: $e');
    }
  }
}
