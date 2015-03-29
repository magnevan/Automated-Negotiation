package negotiator.groupn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import negotiator.Bid;
import negotiator.Domain;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.ValueDiscrete;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Opponent modeling using frequency analysis heuristic
 *
 */
public class GroupnOpponentModel {
    private Map<IssueDiscrete, Map<ValueDiscrete, Integer>> issueValueCount;
    private Map<IssueDiscrete, Double> weights;

    // Number of bids entered into the model.
    // This value is for convenience since the sum of the counts for each
    // issue is equivalent to this
    private int datapoints = 0;

    public GroupnOpponentModel(Domain domain) {
        issueValueCount = new HashMap<>();
        for (Issue issue : domain.getIssues()) {
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            issueValueCount.put(issueDiscrete, new HashMap<>());
            for (ValueDiscrete value : issueDiscrete.getValues()) {
                issueValueCount.get(issueDiscrete).put(value, 0);
            }
        }
    }

    /**
     * Call this when a bid is suggested, or accepted. TODO: Treat bids
     * suggested differently from bids accepted. bids suggested hold more value
     * than bids accepted, also bids accepted tells us something about the
     * opponents acceptance criteria.
     * 
     * TODO: Treat early bids as more important than later bids. It makes sense
     * that the opponent begins with their most valueable bid.
     * 
     * @param bid
     *            the bid that was approved by the opponent
     */
    public void addApprovedBid(Bid bid) {
        try {
            for (Issue issue : bid.getIssues()) {
                ValueDiscrete value = (ValueDiscrete) bid.getValue(issue
                        .getNumber());
                issueValueCount.get(issue).compute(value, (k, v) -> v + 1);
            }
            weights = computeWeights();
            datapoints++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Evaluates the predicted utility of a given bid, using the information
     * provided thus far.
     * 
     * @param bid
     *            the bid to be evaluated
     * @return a value from 0.0 to 1.0 giving the predicted utility of the bid
     */
    public double evaluateBid(Bid bid) {
        double utility = 0.0;

        try {
            for (Issue issueRaw : bid.getIssues()) {
                ValueDiscrete value = (ValueDiscrete) bid.getValue(issueRaw
                        .getNumber());
                IssueDiscrete issue = (IssueDiscrete) issueRaw;

                double issueWeight = weights.get(issue);
                double issueValue = ((double) issueValueCount.get(issue).get(
                        value))
                        / datapoints;

                utility += issueWeight * issueValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return utility;
    }
    
    public JSONObject json() {
        JSONObject obj = new JSONObject();
        
        JSONObject jsonWeights = new JSONObject();
        this.weights.forEach((i, d) -> jsonWeights.put(i.getName(), d));
        obj.put("weights", jsonWeights);
        
        for (IssueDiscrete issue : issueValueCount.keySet()) {
            JSONObject items = new JSONObject();
            issueValueCount.get(issue).forEach((vd, i) -> items.put(vd.getValue(), ((double) i) / datapoints));
            obj.put(issue.getName(), items);
        }
        
        return obj;
    }

    /**
     * Given the current data about the opponent, return the predicted weight
     * for each issue. The weight for each issue is predicted to be determined
     * by the variance in the values selected for that issue. If the opponent
     * suggests many different values for that issue, we assume that it is
     * unimportant for it.
     * 
     * @return a map of issues to their respective weights, normalized such that
     *         the sum of the weights is 1.0
     */
    private Map<IssueDiscrete, Double> computeWeights() {
        Map<IssueDiscrete, Double> weights = new HashMap<>();

        for (IssueDiscrete issue : issueValueCount.keySet()) {
            List<Integer> counts = new ArrayList<>(issueValueCount.get(issue)
                    .values());
            int max = counts.stream().max(Integer::compare).get();
            List<Double> weightedCounts = counts.stream()
                    .mapToDouble(i -> ((double) i / max)).boxed()
                    .collect(Collectors.toList());

            double avg = weightedCounts.stream().reduce(0.0, Double::sum)
                    / weightedCounts.size();
            double variance = weightedCounts.stream()
                    .mapToDouble(d -> (d - avg) * (d - avg)).sum()
                    / weightedCounts.size();
            weights.put(issue, variance);
        }

        // Normalize weights (make them sum to 1.0)
        double sum = weights.values().stream().reduce(0.0, Double::sum);
        weights.forEach((k, v) -> weights.put(k, v / sum));

        return weights;
    }
}
