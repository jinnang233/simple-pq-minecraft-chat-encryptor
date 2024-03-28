# simple-pq-minecraft-chat-encryptor

A chat encryption mod for Minecraft that uses post-quantum KEM CMCE(Classic Mceliece) and post-quantum signature algorithm Falcon-512.

# Mod Dependencies

- Fabric API
- Cloth Config
- Cloth API

# Usage

## Key Pair

Key Pair is automatically generated when ChatHud update. 

After you exchange your public keys, you can send an encrypted message to someone with or without signing.

Public key files is under directory "keys" and end with `.pk` or `.spk`.

## Send an encrypted message to someone without signing

```
etell <Playername> <Message>
```

For example:

```
etell Player166 Hello
```

## Send an encrypted message to someone with signing

```
estell <Playername> <Message>
```

For example:

```
estell Player166 Hello
```



# Warnings

1. I'm not good at coding. The mod may contain bugs. Use it at your own risk.
2. Server may consider these encrypted messages as spam. 
3. Post-quantum algorithms used in these project is **not** formally aduited by cryptographiers. Messages may be cracked in the future.
4. Due to message size problem, the project does **not** use **hybrid** encryption. 



# Credits

This project is impossible without these projects:
- [BouncyCastle](https://github.com/bcgit/bc-csharp)
- [Fabric](https://fabricmc.net/)
- [Fabric API](https://fabricmc.net/)
- [Cloth Config](https://github.com/shedaniel/cloth-config)
- [SnakeYaml](https://github.com/snakeyaml/snakeyaml)
- [fabric_key_binding_api](https://fabricmc.net/)

Thanks to all these projects.

