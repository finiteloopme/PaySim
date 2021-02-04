# Overview

> This projects builds on top of [PaySim repo](https://github.com/finiteloopme/PaySim).  Thank you [Dr. Edgar Lopez-Rojas](http://edgarlopez.net)

The code in this repository is used to generate synthetic (artificial) financial transactions.  
## Aim
1. Generate artificial financial transactions
2. Deploy `PaySim` to [Cloud Run](https://cloud.google.com/run/docs)
3. Publish the transactions to a topic on [Cloud Pubsub](https://cloud.google.com/pubsub/docs/overview)
4. Use dataflow to read transactions from the pubsub topic and write to two destinations (sinks)
   - Cloud SQL
   - Cloud Spanner

```bash
export PROJECT_ID=kl-dev-scratchpad
```
## Acknowledgements
Thank you [Dr. Edgar Lopez-Rojas](http://edgarlopez.net) for open sourcing the [PaySim](https://github.com/finiteloopme/PaySim) code.