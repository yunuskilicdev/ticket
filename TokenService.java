@Service
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final AmazonSQS sqsClient;
    private final String queueUrl;

    // constructor injection for RedisTemplate and AmazonSQS client
    public TokenService(RedisTemplate<String, String> redisTemplate, AmazonSQS sqsClient, String queueUrl) {
        this.redisTemplate = redisTemplate;
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    public boolean validateToken(String tokenId) {
        String accountId = redisTemplate.opsForValue().get(tokenId);
        if (accountId == null) {
            return false;
        }
        sendTravelEventToQueue(accountId, tokenId);
        return true;
    }

    private void sendTravelEventToQueue(String accountId, String tokenId) {
        // create a travel event
        TravelEvent travelEvent = new TravelEvent(accountId,tokenId, new Date(), false);

        // convert travel event to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String travelEventJson;
        try {
            travelEventJson = objectMapper.writeValueAsString(travelEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while converting travel event to JSON", e);
        }

        // send travel event to SQS queue
        SendMessageRequest request = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(travelEventJson);
        sqsClient.sendMessage(request);
    }
}