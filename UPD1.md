<img src="https://github.com/OrdinaryDude/elastic-core-maven/raw/master/html/www/img/nxt_logo.png" alt="Drawing" style="width: 200px;"/>

## A brief summary

I will not go deep into technical jibber jabber here and instead save it for the later sections. Yet, I feel it is important to quickly summarize what we have achieved in the last 2 years. After two years of coding - almost daily - and with the help of many very talended people, we have finally achieved a huge milestone. A milestone that - to my knowledge - nobody has ever achieved before as of now. We as a community have finally managed to launch a first working version of a truly decentralized super-computer - for now, in a test net to be honest - that allows people to submit arbitrary, self-coded jobs to the blockchain and have their tasks solved by so-called "miners" who are incentivized by tit-for-tat payments as they contribute their computational resources towards that task.

At this point I would like to thank everyone who has contributed his time, his resources and his dedication towards this project - without you guys we would not be at the point where we stand right now. I know it has been a hard walk, since many people have treaten us like servants, slaves or, even worse, as pure garbage while all we ever wanted to do is having fun with some fellow travellers on this wonderful journey called XEL, just coding that idea of a super-computer which, let's face it, started very immature in the beginning and gradually improved until it finally became what it is today. Still, there is always room for improvement and there are always new things to add - technology has always been a work in progress and always will be. So I hope that this update attracts more devs who find this project interesting, who genuinely (if you want money, you are the wrong person) want to learn more about XEL's design and who can bring some fresh air into it.

Even though I have everything I need, if you want to help me enjoy life a bit more (e.g., doing that pilot licence, that I have been dreaming of for the last 10 years), feel free (but only if it comes from your heart) to give me a little gift - but make damn sure you do not expect anything in return. No strings (and nothing else crypto related) attached, that is. Will be spent of joyful things and on tax authorities only. And remember, this is not a start-up company or anything, this is a community project comprised of many autonomous agents collaborating together, if you want to support the other wonderful people who helped getting where we are not, please reach out to them.

>XEL: XEL-J8WX-622A-E4TH-GU5YP

>BTC: 18Ed5LQFq3Txs1oDZQtFWpZM4MBvvUvB6Y

By the way, some wonderful people, i guess they are as much of a XEL fanboy as I am, are right now putting together a nice website with lots of more information than you will find here. So be patient and lurk around :)

One little warning though, this is a testnet (with rewards disabled) and we will see things that can be improved, that fail, and things that are perfect. I think it will take not more than a week or so, to get everything sorted out - but you never know :)

Enough of this sentimental talk, let's get started.

## What is a decentralized super-computer?

A super-computer, generally speaking, is a very good scaling distributed computing system which allows for the access to much higher resources that it would be possible on a single dedicated machine. In order to better understand what this is all about, I suggest taking a look at a very good definition of a distributed computing system.

> Distributed computing can be, in the most common sense, understood as a concept where multiple computers are working on one single problem.

> Often, the problem is sub-divided into multiple parts, each solved by a different computer. Typically, the computers able to communicate with each other over the network allowing to synchronize partial results or memory/data among each other. If carried out properly, these computers - to the outside eye - act as a single entity.

> After all, this scheme allows for a massive parallelization and a much faster execution time compared to what would be possible with just one single machine.

That being said, we only need to think about the "decentralization" part. Distributed computing systems in the tradition sense often rely on a central brain, often called the master node, which performs the task splitting, the task distribution and the collection of the results. The absolut cool thing about XEL is, that it has introduced a form of decentralized distributed computing with no central authority. Instead it uses Blockchain technology to form the consensus (that normally would be the task of a master node) and uses a special consensus-based protocol for work distribution, verification and collection.

Now you may ask yourself: "Why do we even need to bother with decentralization? Can't we just use technologies that are already there? I mean, why shouldn't we use already existing centralized solutions as for example BOINC. Well, if you create a project on BOINC, you need to attract people to work on your tasks. If you do not do that, nothing will happen. XEL's apprach is to rely on the Blockchain technology and integrate working on tasks in a way that it is similar to a "mining" process (as known from Bitcoin). Incentivizing by the possible mining rewards, this attracts a whole bunch of crypto affine people to just throw some processing power at you, without the need of you taking care of all that for every single work you post.

## Many other solutions are just disguised as "super-computers"

The blockchain-based super-computer market is emerging (I still think that XEL was the one who started this movement after all *hehe*, but please correct me if I am wrong). In the recent two years multiple approaches have been presented, each of them with its own advantages and disadvantages. When it comes down to executing arbitrarily coded algorithms, the ones I am aware of can be understood as blockchain-driven markets that allow the rental of virtual machine droplets which can then run an arbitrarily coded algorithm. In my personal view, this is just a decentralized variant of DigitalOcean, and has very little in common with a real distributed computer that bundles the processing power of all nodes into one single massive hell-of-a CPU.

However, when it comes down to very specific use cases, other solutions really do provide an excellent performance. One very good example is Golem's rendering functionality, where people can submit a rendering job and have it distributed to a number of nodes which then render a specific area of that image. Approached tailored to (and optimized) a specific use-case will always outperform a general purpose solution. But if you need it general purpose, you will be happy it is there.

## Got scared and ran for the hills playing with other "super-computers"

Just a little story I would like to share. I tried out a different distributed computation solution recently, which allows to run anything on the previously rented virtual server. I kinda liked the idea, i liked it much, until I began to think what could happen if I was the one, who provided his computational resources to the network and others could run whatever they want on my hardware. I would have no problem if people just host a Counterstrike server on my computer and pay me for it ... but I do mind if they set up a hidden tor service, if they run a file sharing server, if they share illegal material or do any other bad stuff that can easily be traced back to my IP address. I certainly do not want that, and running that "node" suddenly seemed a really dangerous thing to do. So I wiped that thing, ditched the bullet and ran for the hills.

Good that XEL is not allowing for that kind of stuff. It comes with an own programming language which can only be used to program "harmless" things which are always safe to be executed by others. You will soon find out what that means.

## A Misconception: XEL is not an Ethereum knock-off

Many people mistake Ethereum for a large super computer suitable for distributed computation tasks. However, this is not the type of problem that Ethereum is meant to solve. Ethereum, to emphasize once again, does not try to exploit the number of nodes in the network for solving certain computations faster.

![Figure 1](https://github.com/OrdinaryDude/elastic-core-maven/raw/master/screens/blk-2.png)

Figure 1 depicts how computations are performed on the Ethereum network. Imagine you have a smart contract in source code form that has been uploaded to the Blockchain; this smart contract is depicted by the upper white rectangle while each node in the Ethereum network is represented by one square in the lower rectangle. The crucial part about understanding the difference between Ethereum and XEL now is to understand that the smart contract’s source code is executed – in its entirety – on every single node in the network. The purpose to do so is to form a strong consensus about the outcome of the result of the execution. Imagine the smart contract is programmed to release a certain amount of Ether, the underlying currency of Ethereum, at a certain Blockchain height to a specific ETH address: when all nodes execute the same code, they can form a consensus about their view on the current state, and – in this case – on the balance of the aforementioned ETH address.

While this type of distributed consensus is very powerful to ensure integrity of code execution across all nodes in the network, it is unsuitable to speed up the execution of complex computation tasks. That is, when there are 1000 equal full-nodes participating in the Ehereum network, then the entire smart contract is executed exactly 1000 times (and not 1000 times faster). This is similar to a more complex variant of the Bitcoin transaction verification method where every node must execute the same verification algorithm (which also is just source code, it is just hard-wired into the Bitcoin client) so that the network as a whole can jointly agree upon whether a transaction was valid and executed or not.

The computation model in XEL is quite different from what we find in Ethereum. Instead of ensuring that every node executes the same code in the same way, the goal is to use the joint computational power of a group of online nodes in order to solve a computation task as fast as possible. Figure 2 depicts the computation model as it is used in XEL. Again – depicted by the upper white rectangle – we start with a problem statement. This will mostly be an algorithm which is meant to solve some arbitrary hard computation task. Then – depicted by the fine grid at the bottom – there is a task splitting strategy: this is a command and conquer strategy, which splits the large problem statement into many small sub-problems. To give you a short example: imagine you have a large unsorted list of numbers and the problem now is to find a particular one in it. The naive approach would be to scan through the entire list element by element and compare it to the number we are looking for. Applying the idea of command and conquer to this problem would result in splitting the huge list into a very large amount of very small lists. These sub-lists then can be searched for the desired number independently – if you now have many nodes, each one working on one of these sub-lists – you can solve the problem a lot quicker. Same happens in XEL: problems are divided into sub-problems and (looking at it from a very high level – a more detailed explanation can be found in the technical details section) “directed” to individual nodes. The main difference is that every node on the XEL network works on a different portion of the greater problem. The result then can be formed by the combination of one or more results to the individual sub-problems. The more nodes work on these sub-problems, the quicker they come to a solution.

## Arbitrary code execution sounds scary - it is not!

XEL is a blockchain based supercomputer that allows the work authors to submit tasks and the workers – incentivized by the attached amount of XEL (the underlying crypto-currency) – to work on these tasks and receive a reimbursement in exchange. This scheme of course requires that the solutions, which are submitted by the network participants, are somehow verified in a way suitable to form a consensus. However, verifying the correctness of solutions to tasks, that have been written by other users, always involves the execution of potentially untrusted code.

At this point, it is important to address other people’s concerns: many people who are running the XEL core client have different motives than posting or solving work tasks. Some may operate a Blockchain explorer, some just want to play around, others want to stash a number of XEL for future use – their motive may vary greatly. And a great deal of these people do not feel comfortable executing any foreign code on their computers. While the computation engine can be turned off and the blockchain is still working, it is no biggie if you leave it on. There is nothing that can happen.

The system is designed in a way, that it is not possible to write malicious code at all. In order to guarantee it, XEL introduces a completely new and hand-crafed pogramming language termed ePL – a novel programming language that allows work authors to express complex algorithms to be solved on the XEL Blockchain with certain restrictions. The language is loosely based on the C programming language incorporating many of the basic logical, bitwise and mathematical operators / functions. More complex operations such as IO operations are not available.

All this means, that some of the operators that we know from C have been removed or altered. For example, there are no GOTO statements and no FOR, WHILE, or DO loops to avoid the possible threat of an indefinitely running loop. Instead, work authors have a repertoire of substitutes that they have to use. Those substitutes are designed in a way, that the “halting problem” is always solvable – that means we know beforehand that the code will terminate and how many instructions it will require in the worst case. These worst case instructions are also bound by an upper limit to avoid programs which “stall” the network for too long. Additionally, there are no pointers which could be used to make the core client crash (SIGTERM) and illegal mathematical operations (such as a division by zero) fall back to a default value.

The ePL code is then converted into C, compiled, and can be quicklt and efficiently executed. The reason why people are not allowed to write their algorithms directly in C should be clear from the explanation above. In C, you can do lots of bad stuff - even corrupt the systems memory. However, ePL code, when transformed into C, will always result in safe code. You can say, to emphasize once again, that the ePL language simply does not allow you to write any program that will either crash (parts of) your system, spy on your system, alter your system in an undesired way or directly cause any other harm to your system simply because it is generally lacking the required operators / functionality.

## Is the testnet really out there?

I understand you want to get your hands on, and the website will contain plenty of information you can read about, so why not taking a first look at the new beauty ... and she is already at a height of 432 blocks and coordinated the execution of a couple first tasks.

<img src="https://github.com/OrdinaryDude/elastic-core-maven/raw/master/screens/testnet.png" alt="Drawing"/>

## How can I set it up?

Setting up a testnet node is only recommended for those who want to start hacking at the system, start digging into the ePL language and start creating first jobs to get a feeling for who everything works. While it is possible to set it up locally on your physical computer (take a look at the Dockerfile to see how it can be done) the recommended way to do it is to use docker.

Assuming you have docker fully set up on your system, all you gotta do is first get the docker repository

```
git clone https://github.com/OrdinaryDude/xel-core-docker.git
cd xel-docker
```

and build the image (which can take quite a while depending on your internet connection)

```
./build.sh
```

You can then start the node by simply issuing

```
./run_foreground.sh
```

You should end up with a running node to which you can connect at

```
http://localhost:16876
```

## But don't I need some XEL to test?

You do need XEL to execute any command on the blockchain. You are free to use, for example, the redeem function to get your testnet XEL from the Genesis block.

Just use "Redeem" button

<img src="https://github.com/OrdinaryDude/elastic-core-maven/raw/master/screens/redeem.png" alt="Drawing"/>

Copy the message to [https://ordinarydude.github.io/offline-bitcoin-signer/ 
](https://ordinarydude.github.io/offline-bitcoin-signer/) and append a " (Evo 5.0.0 Testnet)" to it. This is required to prevent replay attacks between mainnet and testnet

<img src="https://github.com/OrdinaryDude/elastic-core-maven/raw/master/screens/sign.png" alt="Drawing"/>

Then just sign it, copy it back to the redeem dialog, confirm it with your passphrase, and you're good to go.
If you don't want to do that, you can always ask someone for a few testnet XEL. We don't bite.

## Executing your first XEL program

Well, let's go ahead and create our first job. For that, you will need to check out the demo repository

```
git clone https://github.com/OrdinaryDude/xel-demos.git
cd xel-demos
cd 
```

Don't blame me for the retarded "jobs" but they are so simple for a reason. There will be more fancy ones soon. Remember, there are no limits to your imagination. For now, we stick with `simple_job.epl`

<img src="https://github.com/OrdinaryDude/elastic-core-maven/raw/master/screens/sj1.png" alt="Drawing"/>

There will be a more in-depth explanation of the ePL language, the VM interna and everything else that you need to know soon. You could write an entire book about ePL (maybe I will do *hehe*). But for now, we just need to know that m[0]-m[11] are filled by deterministic random integers. An array of 1000 unsigned integers is defined, allowing to store arbitrary values in u[0]-u[999]. A bounty, that is a solution to your problem that you are willing to pay for, is found when the condition in verify_bty() is met. A proof of work payment, usually lower than a bounty payment but needed to keep the crowd around, is found when the hash derived from u[0]-u[4] meets a self-adjusting target value. The main() function is called by the miner, the verify() by the XEL node, allowing for asynchronous work/verify schemes - sometimes calculating something is very hard but verifying it is a lot easier.

To boil that down, a bounty is found when the first random integer is below 1000. Super stupid, but good as a start for new learners.

The demo folder comes with a python-based XEL API, allowing you to post, cancel and monitor jobs. Please study the python files to understand the basic principles of interacting with XEL through a self-written python script.

Let us submit that program to the blockchain by calling

```
python creatework.py
```

This is what you should see

<img src="https://github.com/OrdinaryDude/elastic-core-maven/raw/master/screens/cw.png" alt="Drawing"/>

Let us wait for a confirmation so the work can get included in the blockchain. Let us call

```
python listwork.py
```

to get an overview of all your currently active works

<img src="https://github.com/OrdinaryDude/elastic-core-maven/raw/master/screens/cw2.png" alt="Drawing"/>

You see that - in this case - a new job with id 8400156464018652032 has been created, at the moment with zero bounties, zero pow submissions and a deadline of 250 blocks ahead. After the deadline is met, the job terminates itself.

Time to fire up the miner. Check out the xel_miner repository somewhere (it is cruicial to take exact this version)

```
git clone https://github.com/OrdinaryDude/xel_miner.git
cd xel_miner
```

Maybe, you need some dependencies

```
sudo apt-get install -y libcurl4-openssl-dev cmake build-essential libssl-dev 
```

With your node running, you can start the miner with this command

```
./xel_miner -P PASSPHRASE -o "127.0.0.1:16876" --delaysleep 1 -t 1 
```

with PASSPHRASE being the passphrase of the account where you have your testnet XEL. The -o parameter makes sure to connect to the testnet port, the delaysleep parameter tells to sleep for a second after POW submissions to avoid large bursts in case something goes wrong, and the -t parameter tells the miner to use just one thread.

Once invoked you should see the miner work (and find) solutions or proof of work submissions to your problem

<img src="https://github.com/OrdinaryDude/elastic-core-maven/raw/master/screens/xelminer.png" alt="Drawing"/>

Please don't freak out if you see errors, sometimes - when a new block arrives - old submissions that do not fit the block may overlap the new block. Other than that the internal throttling mechanism may kick in to prevent you to submit too much too often - this would also throw errors. But I agree, xel_miner will need some more minor tweaks because it can crash from time to time. But it does a very decent job so far.

Now issue the listWork command again and see that the number of POW and Bounty submissions have increased, as suggested by the miner. Your solutions are now recorded on the blockchain and can be extracted with the API embedded in the client. This API will be covered in-depth in a separate guide.
 
<img src="https://github.com/OrdinaryDude/elastic-core-maven/raw/master/screens/upd1.png" alt="Drawing"/>

Lets close the job now, you need to put the job id as the argument - make sure you use the correct one

```
python cancelwork.py 
```

Double-checking with the listCommand again, to see if it is really closed?

<img src="https://github.com/OrdinaryDude/elastic-core-maven/raw/master/screens/closed.png" alt="Drawing"/>

The job is closed, nobody can work on it anymore.

Congratulations, you have executed the first job on the XEL blockchain

## Executing your second XEL program, now with distributed storage

Well, the second demo example is a bit trickier. It uses a distributed form of storage that gets synchronized on the XEL computer every x blocks and is being made available to the miners. This is pretty cool, because it allows to "remember" things which then other miners can use to improve their calculations on the problem.

Due to a immense lack of sleep, this guide - including some basic explanations - will appear here tomorrow.

## Bugs

Yup, there will be some, and we - together - will get rid of them, and we will do it fast.

## Hack On

That’s about it. Thanks for listening.
Please keep in mind, it looks so easy because it was made to be used easily. And this was a huge effort by itself. Under the hood, many complex things are happening, and it was a very tough job to make it all work in a decentralized fashion and tamper proof. You will soon learn about all details of XEL, how it is working under the hood, what ePL is capable of, and what limitations you are about to expect - because there are some, simply because of the "better safe than sorry" principle.

I will update this document and improve it tomorrow and in the course of the next week. I have been working 34 hrs straight and really need to get some rest now.


