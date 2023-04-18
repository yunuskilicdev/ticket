@Service
public class TravelService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private AmazonSQSAsync sqsAsyncClient;

    @Value("${aws.sqs.queue.travel-event}")
    private String travelEventQueue;

    @Value("${aws.sqs.queue.ticket-event}")
    private String ticketEventQueue;

    public void processTravelEvent(String message) {
        JSONObject event = new JSONObject(message);
        String tokenId = event.getString("tokenId");
        String accountId = event.getString("accountId");

        // Check if a ticket already exists for this token/account combination
        String ticketKey = accountId + ":" + tokenId;
        String ticketId = redisTemplate.opsForValue().get(ticketKey);

        if (ticketId != null) {
            // A ticket already exists, so this must be a check-out event
            JSONObject ticketEvent = new JSONObject();
            ticketEvent.put("ticketId", ticketId);
            ticketEvent.put("status", "completed");
            sqsAsyncClient.sendMessage(ticketEventQueue, ticketEvent.toString());

            // Remove the ticket ID from Redis
            redisTemplate.delete(ticketKey);
        } else {
            // No ticket exists, so this must be a check-in event
            Ticket ticket = new Ticket(accountId, tokenId);
            // Save the ticket to the database and retrieve its ID
            String newTicketId = saveTicket(ticket);

            // Store the ticket ID in Redis
            redisTemplate.opsForValue().set(ticketKey, newTicketId);

            // Send a ticket creation event to SQS
            JSONObject ticketEvent = new JSONObject();
            ticketEvent.put("ticketId", newTicketId);
            ticketEvent.put("status", "created");
            sqsAsyncClient.sendMessage(ticketEventQueue, ticketEvent.toString());
        }
    }

    private String saveTicket(Ticket ticket) {
        // Persist the ticket to the database and return its ID
        return "1234"; // Dummy ID for demo purposes
    }
}